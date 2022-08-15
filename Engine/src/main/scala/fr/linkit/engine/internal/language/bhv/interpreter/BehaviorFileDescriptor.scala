/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags.{CacheOwnerEngine, CurrentEngine, OwnerEngine, RootOwnerEngine}
import fr.linkit.api.gnom.cache.sync.contract.behavior.{BHVProperties, RMIRulesAgreementBuilder}
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.contract.descriptor._
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo, ModifiableValueContract, SyncLevel}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, LeveledSBDN, MethodContractDescriptorImpl, StructureBehaviorDescriptorNodeImpl}
import fr.linkit.engine.gnom.cache.sync.contract.{FieldContractImpl, SimpleModifiableValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder.EmptyBuilder
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.interpreter.BehaviorFileDescriptor.{DefaultAgreements, DefaultBuilder}
import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.util.ClassMap

import scala.collection.mutable

class BehaviorFileDescriptor(file: BehaviorFile,
                             app: ApplicationContext,
                             propertyClass: BHVProperties,
                             caller: MethodCaller) {

    private val ast                                                            = file.ast
    private val autoChip                                                       = computeOptions()
    private val agreementBuilders: Map[String, RMIRulesAgreementBuilder]       = computeAgreements()
    private val valueModifiers   : Map[String, (Class[_], ValueModifier[Any])] = computeValueModifiers()
    private val typeModifiers    : ClassMap[ValueModifier[AnyRef]]             = computeTypeModifiers()
    private val contracts        : Seq[ContractDescriptorGroup[AnyRef]]        = computeContracts()

    lazy val data = new ContractDescriptorDataImpl(contracts.toArray, ast.fileName) with LangContractDescriptorData {
        override val filePath       = file.filePath
        override val fileName       = ast.fileName
        override val propertiesName = propertyClass.name
    }

    private def computeOptions(): Boolean = {
        ast.options match {
            case AutoChip(enable) :: Nil => enable
            case Nil                     => false
            case options                 =>
                throw new BHVLanguageException(s"option set contains unknown option: '${options}'")
        }
    }

    private def computeContracts(): Seq[ContractDescriptorGroup[AnyRef]] = {
        ast.classDescriptions.groupBy(_.head.classNames.map(file.findClass)).flatMap { case (classes, descs) => {
            val unhandledMethods = mutable.HashMap.empty[AttributedMethodDescription, Boolean]
            val unhandledFields  = mutable.HashMap.empty[AttributedFieldDescription, Boolean]
            val result           = classes.map(computeContract(_, descs)(unhandledMethods, unhandledFields))
            unhandledMethods.filterInPlace((_, b) => !b)
            unhandledFields.filterInPlace((_, b) => !b)
            if (unhandledMethods.nonEmpty || unhandledFields.nonEmpty) {
                val methods = "methods:\n\t" + unhandledMethods.keys.map(_.signature).mkString("\n\t")
                val fields  = "fields:\n\t" + unhandledFields.keys.map(f => f.targetClass.map(_ + ".").getOrElse("") + f.fieldName).mkString("\n\t")
                throw new BHVLanguageException(s"Could not find methods / fields below among classes ${classes.map(_.getSimpleName).mkString(",")}:\n$methods\n$fields")
            }
            result
        }
        }.toSeq :+ new ContractDescriptorGroup[AnyRef] {
            override val clazz       = classOf[Object]
            override val modifier    = None
            override val descriptors = Array(new OverallStructureContractDescriptor[Object] {
                override val autochip    = BehaviorFileDescriptor.this.autoChip
                override val targetClass = classOf[Object]
                override val methods     = Array()
                override val fields      = Array()
            })
        }
    }

    private def computeContract(clazz: Class[_], descs: List[ClassDescription])
                               (unhandledMethods: mutable.HashMap[AttributedMethodDescription, Boolean],
                                unhandledFields: mutable.HashMap[AttributedFieldDescription, Boolean]): ContractDescriptorGroup[AnyRef] = {
        implicit val castedClass = clazz.asInstanceOf[Class[AnyRef]]
        val descriptors0 = descs.flatMap {
            case ClassDescription(ClassDescriptionHead(kind, _), foreachMethod, foreachField, fieldDescs, methodDescs) => {
                val classDesc = kind match {
                    case StaticsDescription => SyncStaticsDescription(clazz)
                    case _                  => SyncObjectDescription(SyncClassDef(clazz))
                }
                implicit val methods0 = describeAttributedMethods(classDesc, kind, foreachMethods(classDesc)(foreachMethod))(methodDescs, unhandledMethods)
                implicit val fields0  = describeAttributedFields(classDesc, kind, foreachFields(classDesc)(foreachField))(fieldDescs, unhandledFields)

                def cast[X](y: Any): X = y.asInstanceOf[X]

                kind match {
                    case StaticsDescription         => List(scd(Statics))
                    case RegularDescription         => List(scd())
                    case LeveledDescription(levels) =>
                        val result = List(scd(levels.map(_.syncLevel).filter(_ != Mirror): _*))
                        levels.find(_.isInstanceOf[MirroringLevel]) match {
                            case None                       => result
                            case Some(MirroringLevel(stub)) =>
                                new MirroringStructureContractDescriptor[AnyRef] {
                                    override val mirroringInfo = MirroringInfo(SyncClassDef(stub.fold(clazz)(cast(file.findClass(_)))))
                                    override val autochip      = BehaviorFileDescriptor.this.autoChip
                                    override val targetClass   = castedClass
                                    override val methods       = methods0
                                    override val fields        = fields0
                                } :: result
                        }
                }
            }
        }
        val group        = new ContractDescriptorGroup[AnyRef] {
            override val clazz      : Class[AnyRef]                              = castedClass
            override val modifier   : Option[ValueModifier[AnyRef]]              = typeModifiers.get(castedClass)
            override val descriptors: Array[StructureContractDescriptor[AnyRef]] = {
                (descriptors0 :+ defaultContracts(clazz, descriptors0)).toArray
            }
        }
        group
    }

    private def defaultContracts(clazz: Class[AnyRef], usedDescs: List[StructureContractDescriptor[AnyRef]]): StructureContractDescriptor[AnyRef] = {
        val missingLevels = StructureBehaviorDescriptorNodeImpl.MandatoryLevels.filterNot(usedDescs.contains)
        scd(missingLevels: _*)(clazz, Array(), Array())
    }

    private def scd(levels: SyncLevel*)(implicit clazz0: Class[AnyRef],
                                        methods0: Array[MethodContractDescriptor],
                                        fields0: Array[FieldContract[Any]]): StructureContractDescriptor[AnyRef] = {
        if (levels.isEmpty) return new OverallStructureContractDescriptor[AnyRef] {
            override val autochip    = BehaviorFileDescriptor.this.autoChip
            override val targetClass = clazz0
            override val methods     = methods0
            override val fields      = fields0
        }
        new MultiStructureContractDescriptor[AnyRef] {
            override val syncLevels  = levels.toSet
            override val autochip    = BehaviorFileDescriptor.this.autoChip
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
                .map(new FieldContractImpl[Any](_, autoChip, desc.state.lvl))
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
                                Some(new SimpleModifiableValueContract[Any](NotRegistered, autoChip, None))
                            else Some(new SimpleModifiableValueContract[Any](rvLvl, autoChip, None))
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
                                         (descriptions: Seq[AttributedMethodDescription],
                                          unhandledMethods: mutable.HashMap[AttributedMethodDescription, Boolean]): Array[MethodContractDescriptor] = {

        val result = descriptions.map { method =>
            val signature     = method.signature
            val targetedClass = signature.target
            if (targetedClass.forall(file.findClass(_).isAssignableFrom(classDesc.specs.mainClass))) {
                unhandledMethods put(method, true)
                describeAttributedMethod(method, classDesc, kind, foreachResult)
            } else {
                if (!unhandledMethods.getOrElse(method, false))
                    unhandledMethods put(method, false)
                null
            }
        }
        foreachResult.filter(_ != null) ++ result.filter(_ != null)
    }

    private def describeAttributedMethod(method: AttributedMethodDescription,
                                         classDesc: SyncStructureDescription[_], kind: DescriptionKind,
                                         foreachResult: Array[MethodContractDescriptor]): MethodContractDescriptor = {
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
                        case RegistrationState(true, state) => new SimpleModifiableValueContract[Any](state, autoChip, rvModifier)
                        case RegistrationState(false, _)    =>
                            val state = referent.flatMap(_.returnValueContract).map(_.registrationKind).getOrElse(NotRegistered)
                            new SimpleModifiableValueContract[Any](state, autoChip, rvModifier)
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
                            new SimpleModifiableValueContract[Any](syncState.lvl, autoChip, modifier)
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

    private def describeAttributedFields(classDesc: SyncStructureDescription[_],
                                         kind: DescriptionKind,
                                         foreachResult: Array[FieldContract[Any]])
                                        (descriptions: Seq[AttributedFieldDescription],
                                         unhandledFields: mutable.HashMap[AttributedFieldDescription, Boolean]): Array[FieldContract[Any]] = {
        val result = descriptions.map { field =>
            if (field.targetClass.forall(file.findClass(_).isAssignableFrom(classDesc.specs.mainClass))) {
                unhandledFields put(field, true)
                val desc        = file.getFieldDescFromName(kind, field.fieldName, classDesc)
                val referentPos = foreachResult.indexWhere(_.description.javaField.getName == field.fieldName)
                val referent    = if (referentPos == -1) None else Some(foreachResult(referentPos))
                if (referentPos != -1)
                    foreachResult(referentPos) = null //removing old version
                field.state match {
                    case RegistrationState(true, isSync) => new FieldContractImpl[Any](desc, autoChip, isSync)
                    case RegistrationState(false, _)     => new FieldContractImpl[Any](desc, autoChip, referent.map(_.registrationKind).getOrElse(NotRegistered))
                }
            } else {
                if (!unhandledFields.getOrElse(field, false))
                    unhandledFields put(field, false)
                null
            }
        }
        foreachResult.filter(_ != null) ++ result.filter(_ != null)
    }

    private def getValueModifier(name: String, expectedType: String): ValueModifier[Any] = {
        val (modTypeTarget, mod) = valueModifiers.getOrElse(name, throw new BHVLanguageException(s"Unknown modifier '$name'"))
        val expectedTypeClass    = file.findClass(expectedType)
        if (!modTypeTarget.isAssignableFrom(expectedTypeClass))
            throw new BHVLanguageException(s"trying to apply a modifier '${modTypeTarget.getName}' on value of type '${expectedTypeClass.getName}'")
        mod
    }

    private def findProcrastinator(properties: Seq[MethodProperty]): Option[Procrastinator] = {
        properties.find(_.name == "executor").map(x => propertyClass.getProcrastinator(x.value))
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
        DefaultAgreements ++ ast.agreementBuilders.map(agreement => {
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

    private final val DefaultBuilder    = new RMIRulesAgreementGenericBuilder().accept(CurrentEngine)
    private final val DefaultAgreements = Map(
        "default" -> DefaultBuilder,
        "only_owner" -> EmptyBuilder
                .accept(OwnerEngine)
                .appointReturn(OwnerEngine),
        "broadcast" -> EmptyBuilder
                .acceptAll()
                .appointReturn(CurrentEngine),
        "only_cache_owner" -> EmptyBuilder
                .accept(CacheOwnerEngine)
                .appointReturn(CacheOwnerEngine),
        "not_current" -> DefaultBuilder
                .acceptAll()
                .discard(CurrentEngine)
                .appointReturn(OwnerEngine),
        "broadcast_if_owner" -> DefaultBuilder
                .assuming(CurrentEngine).is(OwnerEngine, _.acceptAll())
                .accept(CurrentEngine)
                .appointReturn(CurrentEngine),
        "broadcast_if_root_owner" -> DefaultBuilder
                .assuming(CurrentEngine).is(RootOwnerEngine, _.acceptAll())
                .accept(CurrentEngine)
                .appointReturn(CurrentEngine),
        "current_and_owner" -> DefaultBuilder
                .accept(CurrentEngine)
                .accept(OwnerEngine)
                .appointReturn(CurrentEngine)
    )

}