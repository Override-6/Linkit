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
import fr.linkit.api.gnom.cache.sync.contract._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{BHVProperties, RMIRulesAgreementBuilder}
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.contract.descriptor._
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.network.tag.TagSelection._
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, LeveledSBDN, MethodContractDescriptorImpl, StructureBehaviorDescriptorNodeImpl}
import fr.linkit.engine.gnom.cache.sync.contract.{FieldContractImpl, SimpleValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder
import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.interpreter.BehaviorFileInterpreter.{DefaultAgreements, DefaultBuilder, EmptyBuilder}

import scala.collection.mutable

class BehaviorFileInterpreter(file         : BehaviorFile,
                              propertyClass: BHVProperties) {

    private val ast                                                      = file.ast
    private val autoChip                                                 = computeOptions()
    private val agreementBuilders: Map[String, RMIRulesAgreementBuilder] = computeAgreements()
    private val contracts        : Seq[ContractDescriptorGroup[AnyRef]]  = computeContracts()

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
            override val descriptors = Array(new OverallStructureContractDescriptor[Object] {
                override val autochip    = BehaviorFileInterpreter.this.autoChip
                override val targetClass = classOf[Object]
                override val methods     = Array()
                override val fields      = Array()
            })
        }
    }

    private def computeContract(clazz: Class[_], descs: List[ClassDescription])
                               (unhandledMethods: mutable.HashMap[AttributedMethodDescription, Boolean],
                                unhandledFields : mutable.HashMap[AttributedFieldDescription, Boolean]): ContractDescriptorGroup[AnyRef] = {
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
                        val r      = levels.find(_.isInstanceOf[MirroringLevel]) match {
                            case None                       => result
                            case Some(MirroringLevel(stub)) =>
                                new MirroringStructureContractDescriptor[AnyRef] {
                                    override val mirroringInfo = MirroringInfo(SyncClassDef(stub.fold(clazz)(cast(file.findClass(_)))))
                                    override val autochip      = BehaviorFileInterpreter.this.autoChip
                                    override val targetClass   = castedClass
                                    override val methods       = methods0
                                    override val fields        = fields0
                                } :: result
                        }
                        r
                }
            }
        }
        val group        = new ContractDescriptorGroup[AnyRef] {
            override val clazz      : Class[AnyRef]                              = castedClass
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
                                        methods0       : Array[MethodContractDescriptor],
                                        fields0        : Array[FieldContract[Any]]): StructureContractDescriptor[AnyRef] = {
        if (levels.isEmpty) return new OverallStructureContractDescriptor[AnyRef] {
            override val autochip    = BehaviorFileInterpreter.this.autoChip
            override val targetClass = clazz0
            override val methods     = methods0
            override val fields      = fields0
        }
        new MultiStructureContractDescriptor[AnyRef] {
            override val syncLevels  = levels.toSet
            override val autochip    = BehaviorFileInterpreter.this.autoChip
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
                                Some(SimpleValueContract(NotRegistered, autoChip))
                            else Some(SimpleValueContract(rvLvl, autoChip))
                        } else None
                    }

                    val agreement      = desc.agreementOpt.map(getAgreement).getOrElse(DefaultBuilder)
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

    private def describeAttributedMethods(classDesc    : SyncStructureDescription[_], kind: DescriptionKind,
                                          foreachResult: Array[MethodContractDescriptor])
                                         (descriptions    : Seq[AttributedMethodDescription],
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

    private def describeAttributedMethod(method       : AttributedMethodDescription,
                                         classDesc    : SyncStructureDescription[_], kind: DescriptionKind,
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
            if (signature.params.exists(_.tpe.syncType.lvl != NotRegistered))
                throw new BHVLanguageException("disabled methods can't define connected arguments")
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
                    desc.syncReturnValue match {
                        case RegistrationState(true, state) => SimpleValueContract(state, autoChip)
                        case RegistrationState(false, _)    =>
                            val state = referent.flatMap(_.returnValueContract).map(_.registrationKind).getOrElse(NotRegistered)
                            SimpleValueContract(state, autoChip)
                    }
                }
                val agreement          = desc.agreementOpt
                        .map(getAgreement)
                        .orElse(referent.map(_.agreementBuilder))
                        .getOrElse(DefaultBuilder)
                val procrastinator     = findProcrastinator(desc.properties).orElse(referent.flatMap(_.procrastinator))
                val parameterContracts = {
                    val params: Array[ValueContract] = signature.params.map {
                        case MethodParam(_, syncState) =>
                            SimpleValueContract(syncState.syncType.lvl, autoChip)
                    }.toArray
                    //if there is no pertinent information, let's return an empty array in order to inform the system not to worry about the parameters of this method
                    if (params.forall(_.registrationKind == NotRegistered)) params else Array[ValueContract]()
                }
                var invocationMethod   = desc.invocationHandlingMethod
                if (invocationMethod == Inherit)
                    invocationMethod = referent.map(_.invocationHandlingMethod).getOrElse(Inherit)
                MethodContractDescriptorImpl(methodDesc, true, procrastinator, Some(rvContract), parameterContracts, None, invocationMethod, agreement)
        }
        result
    }

    private def describeAttributedFields(classDesc    : SyncStructureDescription[_],
                                         kind         : DescriptionKind,
                                         foreachResult: Array[FieldContract[Any]])
                                        (descriptions   : Seq[AttributedFieldDescription],
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

    private def findProcrastinator(properties: Seq[MethodProperty]): Option[Procrastinator] = {
        properties.find(_.name == "executor").map(x => propertyClass.getProcrastinator(x.value))
    }

    private def getAgreement(name: String): RMIRulesAgreementBuilder = {
        agreementBuilders
                .getOrElse(name, throw new BHVLanguageException(s"undefined agreement '$name'."))
    }

    private def getAgreement(ap: AgreementProvider): RMIRulesAgreementBuilder = {
        ap match {
            case AgreementBuilder(baseAgreement, instructions) =>
                val fib = followInstructionsBased(instructions)(_)
                baseAgreement.map(getAgreement).map(fib).getOrElse(fib(EmptyBuilder))
            case AgreementReference(name)                      => getAgreement(name)
        }
    }

    private def computeAgreements(): Map[String, RMIRulesAgreementBuilder] = {
        DefaultAgreements ++ ast.agreementBuilders.map(agreement => {
            val agName = agreement.agreementName.getOrElse {
                throw new BHVLanguageException("Declared agreement have no name")
            }
            (agName, followInstructions(agreement.instructions))
        })
    }

    private def followInstructions(instructions: Seq[AgreementInstruction]): RMIRulesAgreementBuilder = {
        followInstructionsBased(instructions)(EmptyBuilder)
    }

    private def followInstructionsBased(instructions: Seq[AgreementInstruction])(base: RMIRulesAgreementBuilder): RMIRulesAgreementBuilder = {
        instructions.foldLeft(base)((builder, instruction) => {
            instruction match {
                case DiscardAll                          => builder.selection(Everyone)
                case AcceptAll                           => builder.selection(Nobody)
                case AppointEngine(appointed: UniqueTag) => builder.appointReturn(appointed)
                case AppointEngine(_)                    => throw new BHVLanguageException("Cannot appoint a group or a selection of multiple engines.")
                case AcceptEngines(tags)                 => builder.selection(_ U tags.foldLeft(Select(Nobody): TagSelection[EngineTag])(_ U _))
                case DiscardEngines(tags)                => builder.selection(_ I tags.foldLeft(Select(Nobody): TagSelection[EngineTag])(_ I _))
            }
        })
    }

}

object BehaviorFileInterpreter {

    import TagSelection._

    private final val EmptyBuilder      = new RMIRulesAgreementGenericBuilder()
    private final val DefaultBuilder    = EmptyBuilder.selection(Current)
    private final val DefaultAgreements = Map(
        "default" -> DefaultBuilder,
        "only_owner" -> EmptyBuilder
                .selection(OwnerEngine)
                .appointReturn(OwnerEngine),
        "broadcast" -> EmptyBuilder
                .selection(Everyone)
                .appointReturn(Current),
        "server_only" -> EmptyBuilder
                .selection(Server)
                .appointReturn(Server),
        "not_current" -> DefaultBuilder
                .selection(Everyone - Current)
                .appointReturn(OwnerEngine),
        "current_and_owner" -> DefaultBuilder
                .selection(Current U OwnerEngine)
                .appointReturn(Current)
    )

}