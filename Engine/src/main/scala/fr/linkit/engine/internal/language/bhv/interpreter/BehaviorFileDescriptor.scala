package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags.CurrentEngine
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.contract.descriptor._
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo, ModifiableValueContract, SyncLevel}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, LeveledSBDN, MethodContractDescriptorImpl}
import fr.linkit.engine.gnom.cache.sync.contract.{FieldContractImpl, SimpleModifiableValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder.EmptyBuilder
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.interpreter.BehaviorFileDescriptor.DefaultBuilder
import fr.linkit.engine.internal.language.bhv.{BHVLanguageException, PropertyClass}
import fr.linkit.engine.internal.utils.ClassMap

class BehaviorFileDescriptor(file: BehaviorFile,
                             app: ApplicationContext,
                             propertyClass: PropertyClass,
                             caller: MethodCaller) {
    
    private val ast                                                            = file.ast
    private val agreementBuilders: Map[String, RMIRulesAgreementBuilder]       = computeAgreements()
    private val valueModifiers   : Map[String, (Class[_], ValueModifier[Any])] = computeValueModifiers()
    private val typeModifiers    : ClassMap[ValueModifier[AnyRef]]             = computeTypeModifiers()
    private val contracts        : Seq[ContractDescriptorGroup[AnyRef]]        = computeContracts()
    
    lazy val data = new ContractDescriptorDataImpl(contracts.toArray, ast.fileName) with LangContractDescriptorData {
        override val filePath     : String             = file.filePath
        override val fileName     : String             = ast.fileName
        override val propertyClass: PropertyClass      = BehaviorFileDescriptor.this.propertyClass
        override val app          : ApplicationContext = BehaviorFileDescriptor.this.app
    }
    
    private def computeContracts(): Seq[ContractDescriptorGroup[AnyRef]] = {
        ast.classDescriptions.groupBy(x => file.findClass(x.head.className)).map { case (clazz, descs) => {
            implicit val castedClass = clazz.asInstanceOf[Class[AnyRef]]
            val descriptors0 = descs.map {
                case ClassDescription(ClassDescriptionHead(kind, _), foreachMethod, foreachField, fieldDescs, methodDescs) => {
                    val classDesc = kind match {
                        case StaticsDescription => SyncStaticsDescription(clazz)
                        case _                  => SyncObjectDescription(SyncClassDef(clazz))
                    }
                    implicit val methods0 = describeAttributedMethods(classDesc, kind, foreachMethods(classDesc)(foreachMethod))(methodDescs)
                    implicit val fields0  = describeAttributedFields(classDesc, kind, foreachFields(classDesc)(foreachField))(fieldDescs)
                    
                    def cast[X](y: Any): X = y.asInstanceOf[X]
                    
                    kind match {
                        case StaticsDescription         => scd(Statics)
                        case RegularDescription         => scd()
                        case LeveledDescription(levels) =>
                            levels.find(_.isInstanceOf[MirroringLevel])
                                    .map { case MirroringLevel(stub) =>
                                        MirroringInfo(SyncClassDef(stub.fold(clazz)(cast(file.findClass(_)))))
                                    }
                                    .fold(scd(levels.map(_.syncLevel).filter(_ != Mirror): _*))(
                                        mi => new MirroringStructureContractDescriptor[AnyRef] {
                                            override val mirroringInfo = mi
                                            override val targetClass   = castedClass
                                            override val methods       = methods0
                                            override val fields        = fields0
                                        })
                    }
                }
            }
            new ContractDescriptorGroup[AnyRef] {
                override val clazz      : Class[AnyRef]                              = castedClass
                override val modifier   : Option[ValueModifier[AnyRef]]              = typeModifiers.get(castedClass)
                override val descriptors: Array[StructureContractDescriptor[AnyRef]] = descriptors0.toArray
            }
        }
        }.toSeq
    }
    
    private def scd(levels: SyncLevel*)(implicit clazz0: Class[AnyRef], methods0: Array[MethodContractDescriptor], fields0: Array[FieldContract[Any]]): StructureContractDescriptor[AnyRef] = {
        if (levels.isEmpty) return new OverallStructureContractDescriptor[AnyRef] {
            override val targetClass: Class[AnyRef]                   = clazz0
            override val methods    : Array[MethodContractDescriptor] = methods0
            override val fields     : Array[FieldContract[Any]]       = fields0
        }
        new MultiStructureContractDescriptor[AnyRef] {
            override val syncLevels  = levels.toSet
            override val targetClass = clazz0
            override val methods     = methods0
            override val fields      = fields0
        }
    }
    
    private def foreachFields(classDesc: SyncStructureDescription[_])
                             (descOpt: Option[FieldDescription]): Array[FieldContract[Any]] = {
        if (descOpt.isEmpty)
            return Array()
        val desc = descOpt.get
        classDesc.listFields()
                .map(new FieldContractImpl[Any](_, desc.state.lvl))
                .toArray
    }
    
    private def foreachMethods(classDesc: SyncStructureDescription[_])
                              (descOpt: Option[MethodDescription]): Array[MethodContractDescriptor] = {
        if (descOpt.isEmpty)
            return Array()
        val desc = descOpt.get
        
        def cast[X](y: Any) = y.asInstanceOf[X]
        
        val methods = classDesc match {
            case desc: SyncObjectDescription[_]  => desc.listMethods(cast(desc.specs.mainClass))
            case desc: SyncStaticsDescription[_] => desc.listMethods()
        }
        methods.map { method =>
            desc match {
                case _: DisabledMethodDescription   =>
                    MethodContractDescriptorImpl(method, false, None, None, Array.empty, None, Inherit, DefaultBuilder)
                case desc: EnabledMethodDescription =>
                    val rvContract = {
                        val rvLvl = desc.syncReturnValue.lvl
                        if (rvLvl != NotRegistered) {
                            if (rvLvl.isConnectable && LeveledSBDN.findReasonTypeCantBeSync(method.javaMethod.getReturnType).isDefined)
                                Some(new SimpleModifiableValueContract[Any](NotRegistered, None))
                            else Some(new SimpleModifiableValueContract[Any](rvLvl, None))
                        } else None
                    }
                    
                    val agreement      = desc.agreement.map(ag => getAgreement(ag.name)).getOrElse(DefaultBuilder)
                    val procrastinator = findProcrastinator(desc.properties)
                    MethodContractDescriptorImpl(method, false, procrastinator, rvContract, Array(), None, desc.invocationHandlingMethod, agreement)
                case desc: HiddenMethodDescription  =>
                    val msg = Some(desc.hideMessage.getOrElse(s"${method.javaMethod} is hidden"))
                    MethodContractDescriptorImpl(method, false, None, None, Array(), msg, Inherit, DefaultBuilder)
            }
        }.toArray
    }
    
    private def encodedIntMethodString(i: Int): String = {
        if (i > 0) i.toString
        else "_" + i.abs.toString
    }
    
    private def describeAttributedMethods(classDesc: SyncStructureDescription[_], kind: DescriptionKind,
                                          foreachResult: Array[MethodContractDescriptor])
                                         (descriptions: Seq[AttributedMethodDescription]): Array[MethodContractDescriptor] = {
        
        val result = descriptions.map { method =>
            val signature  = method.signature
            val methodDesc = file.getMethodDescFromSignature(kind, signature, classDesc)
            
            val referentPos = foreachResult.indexWhere(x => x != null && x.description == methodDesc)
            val referent    = if (referentPos == -1) None else Some(foreachResult(referentPos))
            if (referentPos >= 0) {
                //removing description in foreach statement result
                foreachResult(referentPos) = null
            }
            
            def checkParams(): Unit = {
                if (signature.params.exists(_.syncState.lvl != NotRegistered))
                    throw new BHVLanguageException("disabled methods can't define connected arguments")
            }
            
            def extractModifier(mod: CompModifier, valType: String): ValueModifier[Any] = {
                mod match {
                    case ValueModifierReference(_, ref) => getValueModifier(ref, valType)
                    case ModifierExpression(_, in, out) => makeModifier(
                        s"method_${encodedIntMethodString(methodDesc.methodId)}", valType, in, out)
                }
            }
            
            val result = (method: MethodDescription) match {
                case _: DisabledMethodDescription  =>
                    checkParams()
                    MethodContractDescriptorImpl(methodDesc, true, None, None, Array.empty, None, Inherit, DefaultBuilder)
                case desc: HiddenMethodDescription =>
                    checkParams()
                    val msg = Some(desc.hideMessage.getOrElse(s"${methodDesc.javaMethod} is hidden"))
                    MethodContractDescriptorImpl(methodDesc, true, None, None, Array(), msg, Inherit, DefaultBuilder)
                
                case desc: EnabledMethodDescription with AttributedEnabledMethodDescription =>
                    val rvContract         = {
                        val returnTypeName = methodDesc.javaMethod.getReturnType.getName
                        val rvModifier     = desc.modifiers.find(_.target == "returnvalue").map(extractModifier(_, returnTypeName))
                        desc.syncReturnValue match {
                            case RegistrationState(true, state) => new SimpleModifiableValueContract[Any](state, rvModifier)
                            case RegistrationState(false, _)    =>
                                val state = referent.flatMap(_.returnValueContract).map(_.registrationKind).getOrElse(NotRegistered)
                                new SimpleModifiableValueContract[Any](state, rvModifier)
                        }
                    }
                    val agreement          = desc.agreement
                            .map(ag => getAgreement(ag.name))
                            .orElse(referent.map(_.agreementBuilder))
                            .getOrElse(DefaultBuilder)
                    val procrastinator     = findProcrastinator(desc.properties).orElse(referent.flatMap(_.procrastinator))
                    val parameterContracts = {
                        val acc: Array[ModifiableValueContract[Any]] = signature.params.map {
                            case MethodParam(syncState, nameOpt, tpe) =>
                                val modifier = nameOpt.flatMap(name => desc.modifiers.find(_.target == name).map(extractModifier(_, tpe)))
                                new SimpleModifiableValueContract[Any](syncState.lvl, modifier)
                        }.toArray
                        if (acc.exists(x => x.registrationKind != NotRegistered || x.modifier.isDefined)) acc else Array[ModifiableValueContract[Any]]()
                    }
                    var invocationMethod   = desc.invocationHandlingMethod
                    if (invocationMethod == Inherit)
                        invocationMethod = referent.map(_.invocationHandlingMethod).getOrElse(Inherit)
                    MethodContractDescriptorImpl(methodDesc, true, procrastinator, Some(rvContract), parameterContracts, None, invocationMethod, agreement)
            }
            result
        }
        foreachResult.filter(_ != null) ++ result
    }
    
    private def describeAttributedFields(classDesc: SyncStructureDescription[_],
                                         kind: DescriptionKind,
                                         foreachResult: Array[FieldContract[Any]])
                                        (descriptions: Seq[AttributedFieldDescription]): Array[FieldContract[Any]] = {
        val result = descriptions.map { field =>
            val desc        = file.getFieldDescFromName(kind, field.fieldName, classDesc)
            val referentPos = foreachResult.indexWhere(_.description.javaField.getName == field.fieldName)
            val referent    = if (referentPos == -1) None else Some(foreachResult(referentPos))
            if (referentPos != -1)
                foreachResult(referentPos) = null //removing old version
            field.state match {
                case RegistrationState(true, isSync) => new FieldContractImpl[Any](desc, isSync)
                case RegistrationState(false, _)     => new FieldContractImpl[Any](desc, referent.map(_.registrationKind).getOrElse(NotRegistered))
            }
        }
        foreachResult.filter(_ != null) ++ result
    }
    
    private def getValueModifier(name: String, expectedType: String): ValueModifier[Any] = {
        val (modTypeTarget, mod) = valueModifiers.getOrElse(name, throw new BHVLanguageException(s"Unknown modifier '$name'"))
        val expectedTypeClass    = file.findClass(expectedType)
        if (!modTypeTarget.isAssignableFrom(expectedTypeClass))
            throw new BHVLanguageException(s"trying to apply a modifier '${modTypeTarget.getName}' on value of type '${expectedTypeClass.getName}'")
        mod
    }
    
    private def findProcrastinator(properties: Seq[MethodProperty]): Option[Procrastinator] = {
        properties.find(_.name == "procrastinator").map(x => propertyClass.getProcrastinator(x.value))
    }
    
    private def getAgreement(name: String): RMIRulesAgreementBuilder = {
        agreementBuilders
                .getOrElse(name, throw new BHVLanguageException(s"undefined agreement '$name'."))
    }
    
    private def computeTypeModifiers(): ClassMap[ValueModifier[AnyRef]] = {
        val map = ast.typesModifiers.map {
            case TypeModifier(className, in, out) => {
                (file.findClass(className), makeModifier[AnyRef]("type", className, in, out))
            }
        }.toMap
        new ClassMap(map)
    }
    
    private def computeValueModifiers(): Map[String, (Class[_], ValueModifier[Any])] = {
        ast.valueModifiers.map {
            case ValueModifier(name, tpeName, in, out) =>
                (name, (file.findClass(tpeName), makeModifier[Any](s"value_$name", tpeName, in, out)))
        }.toMap
    }
    
    private def makeModifier[A](tpe: String, valTpeName: String, in: Option[LambdaExpression], out: Option[LambdaExpression]): ValueModifier[A] = {
        val formattedTpeName = file.formatClassName(file.findClass(valTpeName))
        val inLambdaName     = s"${tpe}_in_${formattedTpeName}"
        val outLambdaName    = s"${tpe}_out_${formattedTpeName}"
        new ValueModifier[A] {
            
            override def fromRemote(input: A, remote: Engine): A = {
                if (in.isDefined) {
                    val result = caller.call(inLambdaName, Array(input, remote))
                    if (result == ()) input else result.asInstanceOf[A]
                } else input
            }
            
            override def toRemote(input: A, remote: Engine): A = {
                if (out.isDefined) {
                    val result = caller.call(outLambdaName, Array(input, remote))
                    if (result == ()) input else result.asInstanceOf[A]
                } else input
            }
        }
    }
    
    private def computeAgreements(): Map[String, RMIRulesAgreementBuilder] = {
        ast.agreementBuilders.map(agreement => {
            (agreement.name, followInstructions(agreement.instructions))
        }).toMap
    }
    
    private def followInstructions(instructions: Seq[AgreementInstruction]): RMIRulesAgreementBuilder = {
        def follow(instructions: Seq[AgreementInstruction], base: RMIRulesAgreementBuilder): RMIRulesAgreementBuilder = {
            instructions.foldLeft(base)((builder, instruction) => {
                instruction match {
                    case DiscardAll               => builder.discardAll()
                    case AcceptAll                => builder.acceptAll()
                    case AppointEngine(appointed) => builder.appointReturn(appointed)
                    case AcceptEngines(tags)      => tags.foldLeft(builder) { case (builder, tag) => builder.accept(tag) }
                    case DiscardEngines(tags)     => tags.foldLeft(builder) { case (builder, tag) => builder.discard(tag) }
                    
                    case Condition(Equals(a, b, false), ifTrue, ifFalse) =>
                        (builder assuming a isElse b) (follow(ifTrue, _), follow(ifFalse, _))
                    case Condition(Equals(a, b, true), ifTrue, ifFalse)  =>
                        (builder assuming a isNotElse b) (follow(ifTrue, _), follow(ifFalse, _))
                }
            })
        }
        
        follow(instructions, EmptyBuilder)
    }
    
}

object BehaviorFileDescriptor {
    
    private final val DefaultBuilder = new RMIRulesAgreementGenericBuilder().accept(CurrentEngine)
}