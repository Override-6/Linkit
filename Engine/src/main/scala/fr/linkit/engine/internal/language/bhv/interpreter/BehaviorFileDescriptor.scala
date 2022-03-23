package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{MethodContractDescriptor, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, ModifiableValueContract, RemoteObjectInfo}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, MethodContractDescriptorImpl}
import fr.linkit.engine.gnom.cache.sync.contract.{FieldContractImpl, SimpleModifiableValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder.EmptyBuilder
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.integration.LambdaCaller
import fr.linkit.engine.internal.language.bhv.{BHVLanguageException, PropertyClass}
import fr.linkit.engine.internal.utils.ClassMap

import java.net.URL

class BehaviorFileDescriptor(file: BehaviorFile, app: ApplicationContext, propertyClass: PropertyClass, caller: LambdaCaller) {

    private val ast                                                            = file.ast
    private val agreementBuilders: Map[String, RMIRulesAgreementBuilder]       = computeAgreements()
    private val valueModifiers   : Map[String, (Class[_], ValueModifier[Any])] = computeValueModifiers()
    private val typeModifiers    : ClassMap[ValueModifier[AnyRef]]             = computeTypeModifiers()
    private val contracts        : Seq[StructureContractDescriptor[_]]         = computeContracts()

    lazy val data = {
        new ContractDescriptorDataImpl(contracts.toArray) with LangContractDescriptorData {
            override val filePath     : String             = file.source
            override val propertyClass: PropertyClass      = BehaviorFileDescriptor.this.propertyClass
            override val app          : ApplicationContext = BehaviorFileDescriptor.this.app
        }
    }

    private def computeContracts(): Seq[StructureContractDescriptor[_]] = {
        val result = ast.classDescriptions.map {
            case ClassDescription(ClassDescriptionHead(kind, className), foreachMethod, foreachField, fieldDescs, methodDescs) => {
                val clazz                      = file.findClass(className)
                val (mirroringInfo, classDesc) = kind match {
                    case RegularDescription         => (None, SyncObjectDescription(clazz))
                    case StaticsDescription         => (None, SyncStaticsDescription(clazz))
                    case MirroringDescription(stub) => (Some(RemoteObjectInfo(file.findClass(stub))), SyncObjectDescription(clazz))
                }
                new StructureContractDescriptor[AnyRef] {
                    override val targetClass      = clazz.asInstanceOf[Class[AnyRef]]
                    override val remoteObjectInfo = mirroringInfo
                    override val methods          = describeAttributedMethods(classDesc, kind, foreachMethods(classDesc)(foreachMethod))(methodDescs)
                    override val fields           = describeAttributedFields(classDesc, kind, foreachFields(classDesc)(foreachField))(fieldDescs)
                    override val modifier         = typeModifiers.get(clazz)
                }
            }
        }
        new StructureContractDescriptor[AnyRef] {
            override val targetClass      = classOf[Object]
            override val remoteObjectInfo = None
            override val methods          = Array()
            override val fields           = Array()
            override val modifier         = None
        } :: result
    }

    private def foreachFields(classDesc: SyncStructureDescription[_])
                             (descOpt: Option[FieldDescription]): Array[FieldContract[Any]] = {
        if (descOpt.isEmpty)
            return Array()
        val desc = descOpt.get
        classDesc.listFields()
                .map(new FieldContractImpl[Any](_, desc.state.isSync))
                .toArray
    }

    private def foreachMethods(classDesc: SyncStructureDescription[_])
                              (descOpt: Option[MethodDescription]): Array[MethodContractDescriptor] = {
        if (descOpt.isEmpty)
            return Array()
        val desc = descOpt.get
        classDesc.listMethods().map { method =>
            desc match {
                case _: DisabledMethodDescription   =>
                    MethodContractDescriptorImpl(method, false, None, None, Array.empty, None, false, EmptyBuilder)
                case desc: EnabledMethodDescription =>
                    val rvContract     = if (desc.syncReturnValue.isSync) Some(new SimpleModifiableValueContract[Any](true, None)) else None
                    val agreement      = desc.agreement.map(ag => getAgreement(ag.name)).getOrElse(EmptyBuilder)
                    val procrastinator = findProcrastinator(desc.properties)
                    MethodContractDescriptorImpl(method, false, procrastinator, rvContract, Array(), None, false, agreement)
                case desc: HiddenMethodDescription  =>
                    val msg = Some(desc.hideMessage.getOrElse(s"${method.javaMethod} is hidden"))
                    MethodContractDescriptorImpl(method, false, None, None, Array(), msg, false, EmptyBuilder)
            }
        }.toArray
    }

    private def describeAttributedMethods(classDesc: SyncStructureDescription[_], kind: DescriptionKind,
                                          foreachResult: Array[MethodContractDescriptor])
                                         (descriptions: Seq[AttributedMethodDescription]): Array[MethodContractDescriptor] = {

        val result = descriptions.map { method =>
            val signature  = method.signature
            val methodDesc = file.getMethodDescFromSignature(kind, signature, classDesc)

            val referentPos = foreachResult.indexWhere(_.description == methodDesc)
            val referent    = if (referentPos == -1) None else Some(foreachResult(referentPos))
            if (referentPos >= 0) {
                //removing description in foreach statement result
                foreachResult(referentPos) = null
            }

            def checkParams(): Unit = {
                if (signature.params.exists(_.syncState.isSync))
                    throw new BHVLanguageException("disabled methods can't define synchronized arguments")
            }

            def extractModifier(mod: CompModifier, valType: String): ValueModifier[Any] = {
                mod match {
                    case ValueModifierReference(_, ref)      => getValueModifier(ref, valType)
                    case ModifierExpression(target, in, out) => makeModifier(s"method_${target}_${signature.methodName}_${signature.hashCode()}", valType, in, out)
                }
            }

            val result = (method: MethodDescription) match {
                case _: DisabledMethodDescription  =>
                    checkParams()
                    MethodContractDescriptorImpl(methodDesc, true, None, None, Array.empty, None, false, EmptyBuilder)
                case desc: HiddenMethodDescription =>
                    checkParams()
                    val msg = Some(desc.hideMessage.getOrElse(s"${methodDesc.javaMethod} is hidden"))
                    MethodContractDescriptorImpl(methodDesc, true, None, None, Array(), msg, false, EmptyBuilder)

                case desc: EnabledMethodDescription with AttributedEnabledMethodDescription =>
                    val rvContract         = {
                        val returnTypeName = methodDesc.javaMethod.getReturnType.getName
                        val rvModifier     = desc.modifiers.find(_.target == "returnvalue").map(extractModifier(_, returnTypeName))
                        desc.syncReturnValue match {
                            case SynchronizeState(true, isSync) => new SimpleModifiableValueContract[Any](isSync, rvModifier)
                            case SynchronizeState(false, _)     => new SimpleModifiableValueContract[Any](referent.flatMap(_.returnValueContract).exists(_.isSynchronized), rvModifier)
                        }
                    }
                    val agreement          = desc.agreement
                            .map(ag => getAgreement(ag.name))
                            .orElse(referent.map(_.agreement))
                            .getOrElse(EmptyBuilder)
                    val procrastinator     = findProcrastinator(desc.properties).orElse(referent.flatMap(_.procrastinator))
                    val parameterContracts = {
                        val acc: Array[ModifiableValueContract[Any]] = signature.params.map {
                            case MethodParam(syncState, nameOpt, tpe) =>
                                val modifier = nameOpt.flatMap(name => desc.modifiers.find(_.target == name).map(extractModifier(_, tpe)))
                                new SimpleModifiableValueContract[Any](syncState.isSync, modifier)
                        }.toArray
                        if (!acc.exists(x => x.isSynchronized || x.modifier.isDefined)) Array[ModifiableValueContract[Any]]() else acc
                    }
                    MethodContractDescriptorImpl(methodDesc, true, procrastinator, Some(rvContract), parameterContracts, None, false, agreement)
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
            val referentPos = foreachResult.indexWhere(_.desc.javaField.getName == field.fieldName)
            val referent    = if (referentPos == -1) None else Some(foreachResult(referentPos))
            if (referentPos != -1)
                foreachResult(referentPos) = null //removing old version
            field.state match {
                case SynchronizeState(true, isSync) => new FieldContractImpl[Any](desc, isSync)
                case SynchronizeState(false, _)     => new FieldContractImpl[Any](desc, referent.exists(_.isSynchronized))
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

                    case Condition(Equals(a, b, false), ifTrue, ifFalse) => (builder assuming a isElse b) (follow(ifTrue, _), follow(ifFalse, _))
                    case Condition(Equals(a, b, true), ifTrue, ifFalse)  => (builder assuming a isNotElse b) (follow(ifTrue, _), follow(ifFalse, _))
                }
            })
        }

        follow(instructions, EmptyBuilder)
    }

}

