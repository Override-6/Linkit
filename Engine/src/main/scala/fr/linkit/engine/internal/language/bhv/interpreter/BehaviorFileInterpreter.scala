package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncStructureDescription, FieldDescription => SFieldDescription, MethodDescription => SMethodDescription}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, MethodContractDescriptor, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, ModifiableValueContract, RemoteObjectInfo}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.{ContractDescriptorDataImpl, MethodContractDescriptorImpl}
import fr.linkit.engine.gnom.cache.sync.contract.{FieldContractImpl, SimpleModifiableValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder.EmptyBuilder
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.compilation.FileIntegratedLambdas
import fr.linkit.engine.internal.language.bhv.{BHVLanguageException, PropertyClass}
import fr.linkit.engine.internal.utils.ClassMap

import java.lang.reflect.Modifier
import scala.util.Try

class BehaviorFileInterpreter(ast: BehaviorFile, fileName: String, center: CompilerCenter, propertyClass: PropertyClass) {

    if (propertyClass == null)
        throw new NullPointerException("property class cannot be null. ")
    private val imports          : Map[String, Class[_]]                       = computeImports()
    private val lambdas          : FileIntegratedLambdas                       = new FileIntegratedLambdas(fileName, center, imports.values.toSeq, ast.codeBlocks.map(_.sourceCode))
    private val agreementBuilders: Map[String, RMIRulesAgreementBuilder]       = computeAgreements()
    private val valueModifiers   : Map[String, (Class[_], ValueModifier[Any])] = computeValueModifiers()
    private val typeModifiers    : ClassMap[ValueModifier[AnyRef]]             = computeTypeModifiers()
    private val contracts        : Seq[StructureContractDescriptor[_]]         = computeContracts()

    lazy val contractDescriptorData = {
        lambdas.compileLambdas(propertyClass)
        new ContractDescriptorDataImpl(contracts.toArray)
    }

    def getData: ContractDescriptorData = contractDescriptorData

    private def computeContracts(): Seq[StructureContractDescriptor[_]] = {
        ast.classDescriptions.map {
            case ClassDescription(ClassDescriptionHead(kind, className), foreachMethod, foreachField, fieldDescs, methodDescs) => {
                val clazz                      = findClass(className)
                val (mirroringInfo, classDesc) = kind match {
                    case RegularDescription         => (None, SyncObjectDescription(clazz))
                    case StaticsDescription         => (None, SyncStaticDescription(clazz))
                    case MirroringDescription(stub) => (Some(RemoteObjectInfo(findClass(stub))), SyncObjectDescription(clazz))
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
            val methodDesc = getMethodDescFromSignature(kind, signature, classDesc)

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

            def extractModifier(mod: CompModifier, returnType: String): ValueModifier[Any] = {
                mod match {
                    case ValueModifierReference(_, ref) => getValueModifier(ref, returnType)
                    case ModifierExpression(_, in, out) => makeModifier(returnType, in, out)
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
            val desc        = getFieldDescFromName(kind, field.fieldName, classDesc)
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

    private def getMethodDescFromSignature(kind: DescriptionKind, signature: MethodSignature, classDesc: SyncStructureDescription[_]): SMethodDescription = {
        val name   = signature.methodName
        val params = signature.params.map(param => findClass(param.tpe)).toArray
        val method = {
            try classDesc.clazz.getDeclaredMethod(name, params: _*)
            catch {
                case _: NoSuchMethodException => throw new BHVLanguageException(s"Unknown method $signature in ${classDesc.clazz}")
            }
        }
        val static = Modifier.isStatic(method.getModifiers)
        //checking if method is valid depending on the description context
        kind match {
            case StaticsDescription if !static                            => throw new BHVLanguageException(s"Method '$signature' is not static.")
            case _@MirroringDescription(_) | RegularDescription if static => throw new BHVLanguageException(s"Method '$signature' is static. ")
            case _                                                        =>
        }

        val methodID = SMethodDescription.computeID(method)
        classDesc.findMethodDescription(methodID).get
    }

    private def getFieldDescFromName(kind: DescriptionKind, name: String, classDesc: SyncStructureDescription[_]): SFieldDescription = {
        val clazz  = classDesc.clazz
        val field  = {
            try clazz.getDeclaredField(name)
            catch {
                case _: NoSuchFieldException => throw new BHVLanguageException(s"Unknown field $name in ${clazz}")
            }
        }
        val static = Modifier.isStatic(field.getModifiers)
        kind match {
            case StaticsDescription if !static                            => throw new BHVLanguageException(s"Field $name is not static.")
            case _@MirroringDescription(_) | RegularDescription if static => throw new BHVLanguageException(s"Field $name is static. ")
            case _                                                        =>
        }
        classDesc.findFieldDescription(SFieldDescription.computeID(field)).get
    }

    private def getValueModifier(name: String, expectedType: String): ValueModifier[Any] = {
        val (modTypeTarget, mod) = valueModifiers.getOrElse(name, throw new BHVLanguageException(s"Unknown modifier '$name'"))
        val expectedTypeClass    = findClass(expectedType)
        if (!modTypeTarget.isAssignableFrom(expectedTypeClass))
            throw new BHVLanguageException(s"trying to apply a modifier '${modTypeTarget.getName}' on value of type '${expectedTypeClass.getName}'")
        mod
    }

    private def findProcrastinator(properties: Seq[MethodProperty]): Option[Procrastinator] = {
        properties.find(_.name == "procrastinator").map(x => propertyClass.getProcrastinator(x.value))
    }

    private def findClass(name: String): Class[_] = {
        var arrayIndex = name.indexOf('[')
        if (arrayIndex == -1) arrayIndex = name.length
        val pureName   = name.take(arrayIndex)
        val arrayDepth = (name.length - pureName.length) / 2
        val clazz      = imports
            .get(pureName)
            .orElse(Try(Class.forName(pureName)).toOption)
            .orElse(Try(Class.forName("java.lang." + pureName)).toOption)
            .getOrElse {
                throw new BHVLanguageException(s"Unknown class '$pureName' ${if (pureName.contains(".")) ", is it imported ?" else ""}")
            }
        (0 until arrayDepth).foldLeft[Class[_]](clazz)((cl, _) => cl.arrayType())
    }

    private def getAgreement(name: String): RMIRulesAgreementBuilder = {
        agreementBuilders
            .getOrElse(name, throw new BHVLanguageException(s"undefined agreement '$name'."))
    }

    private def computeTypeModifiers(): ClassMap[ValueModifier[AnyRef]] = {
        val map = ast.typesModifiers.map {
            case TypeModifier(className, in, out) => {
                (findClass(className), makeModifier[AnyRef](className, in, out))
            }
        }.toMap
        new ClassMap(map)
    }

    private def computeValueModifiers(): Map[String, (Class[_], ValueModifier[Any])] = {
        ast.valueModifiers.map {
            case ValueModifier(name, tpeName, in, out) =>
                (name, (findClass(tpeName), makeModifier[Any](tpeName, in, out)))
        }.toMap
    }

    private def makeModifier[A](valTpeName: String, in: Option[LambdaExpression], out: Option[LambdaExpression]): ValueModifier[A] = {
        val clazz     = findClass(valTpeName)
        val inLambda  = in.map(in => lambdas.submitLambda(in.block.sourceCode, clazz, classOf[Engine])).orNull
        val outLambda = out.map(out => lambdas.submitLambda(out.block.sourceCode, clazz, classOf[Engine])).orNull
        new ValueModifier[A] {
            override def fromRemote(input: A, remote: Engine): A = {
                if (inLambda != null) {
                    val result = inLambda(Array(input, remote))
                    if (result == ()) input else result
                } else input
            }

            override def toRemote(input: A, remote: Engine): A = {
                if (outLambda != null) {
                    val result = outLambda(Array(input, remote))
                    if (result == ()) input else result
                } else input
            }
        }
    }

    private def computeImports(): Map[String, Class[_]] = {
        ast.classImports.map { x =>
            val clazz = Class.forName(x.className)
            (clazz.getSimpleName, clazz)
        }.toMap
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

