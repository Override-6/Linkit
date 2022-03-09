/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.cache.sync.contract.ValueContract
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncStructureDescription, MethodDescription => MethodDesc}
import fr.linkit.api.gnom.cache.sync.contract.descriptors.{ContractDescriptorData, MethodContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.gnom.cache.sync.contract.SimpleValueContract
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.MethodContractDescriptorImpl
import fr.linkit.engine.gnom.cache.sync.contract.modification.LambdaValueModifier
import fr.linkit.engine.gnom.cache.sync.invokation.GenericRMIRulesAgreementBuilder
import fr.linkit.engine.internal.language.bhv.compilation.FileIntegratedLambdas
import fr.linkit.engine.internal.language.bhv.parser.ASTTokens._

import scala.collection.mutable
import scala.reflect.ClassTag

class BHVLangTokenInterpreter(tokens: List[RootToken], fileName: String,
                              center: CompilerCenter, properties: PropertyClass) {

    private val importedClasses = mutable.HashMap.empty[String, Class[_]]
    private val lambdas         = new FileIntegratedLambdas(fileName, center)


    private lazy val data: ContractDescriptorData = {
        computeData()
        lambdas.compileLambdas()
        null
    }

    def getData: ContractDescriptorData = {
        data
    }

    private def computeData(): Unit = {
        tokens.foreach {
            case ImportToken(name)                   => importClass(name)
            case ClassDescription(head, foreachMethod,
            foreachField, methods, fields)           => classDesc(head, foreachMethod, foreachField, methods, fields)
            case ScalaCodeBlock(code)                =>
            case TypeModifiers(className, modifiers) =>
        }
    }

    private def classDesc(head: ClassDescriptionHead, foreachMethod: Option[MethodDescription], foreachField: Option[FieldDescription],
                          methodSeq: Seq[MethodDescription], fieldSeq: Seq[FieldDescription]): Unit = {
        val clazz = findClass(head.className)
        val desc  = if (head.static) SyncStaticDescription(clazz) else SyncObjectDescription(clazz)
        /*new StructureContractDescriptor[_] {
            override val targetClass     : Class[_]                         = clazz
            override val remoteObjectInfo: Option[RemoteObjectInfo]         = head.remoteObjectInfo
            override val methods         : Array[MethodContractDescriptor]  = methodSeq.map(methodDesc(desc, _)).toArray
            override val fields          : Array[(Int, FieldContract[Any])] = ???
            override val modifier        : Option[ValueModifier[_]]         = ???
        }*/
    }

    private def methodDesc(ssd: SyncStructureDescription[_ <: AnyRef],
                           desc: MethodDescription): MethodContractDescriptor = desc match {
        case DisabledMethodDescription(Some(signature))        =>
            disabledMethodDesc(signature, None, ssd)
        case HiddenMethodDescription(Some(signature), hideMsg) =>
            val hideMessage = hideMsg.getOrElse(s"Method ${ssd.clazz.getName}.$signature is hidden.")
            disabledMethodDesc(signature, Some(hideMessage), ssd)
        case EnabledMethodDescription(referent, procrastinatorName,
        Some(signature), modifiers, syncReturnValue)           => ???

    }

    private def enabledMethodDesc(referent: Option[ValueReference],
                                  procrastinatorName: Option[ExternalReference],
                                  signature: MethodSignature,
                                  modifiers: Seq[MethodComponentsModifier],
                                  syncReturnValue: SynchronizeState,
                                  ssd: SyncStructureDescription[_ <: AnyRef]): Unit = {
        val procrastinator     = ??? //procrastinatorName.map(properties.getProcrastinator)
        val clazz              = ssd.clazz
        val rvContract         = getContract(modifiers, signature, syncReturnValue.value, clazz)("return value", _.returnvalueModifiers)
        val parameterContracts = {
            for (idx <- signature.params.indices) yield {
                val param  = signature.params(idx)
                val isSync = param.synchronized
                getContract(modifiers, signature, isSync, clazz)(s"parameter $idx", _.paramsModifiers.getOrElse(idx, Seq()))
            }
        }.toArray

        val javaMethod = clazz.getMethod(signature.methodName, signature.params.map(p => findClass(p.tpe)): _*)
        val desc       = new MethodDesc(javaMethod, ssd)
        val builder    = new GenericRMIRulesAgreementBuilder()
        ??? //MethodContractDescriptorImpl(desc, procrastinator, Some(rvContract), parameterContracts, None, ???, builder)
    }

    private def getContract(modifiers: Seq[MethodComponentsModifier],
                            signature: MethodSignature,
                            sync: Boolean,
                            clazz: Class[_])
                           (compName: String, extract: MethodComponentsModifier => Seq[LambdaExpression]): ValueContract[AnyRef] = {
        val filteredMods = modifiers.filter(extract(_).nonEmpty)
            .flatMap(extract)
            .foldRight(List[LambdaExpression]())((mod, acc) => {
                if (acc.contains(mod.kind)) throw new ModifierConflictException(s"Multiple modifiers set for method $compName ${clazz.getName}.${signature}.")
                mod :: acc
            })
        //                 use a light implementation of ValueModifier if no modifier is set
        val modifier     = if (filteredMods.isEmpty) new ValueModifier[AnyRef] {} else {
            new LambdaValueModifier[AnyRef] {
                filteredMods.foreach(lamb => lamb.kind match {
                    case CurrentToRemoteEvent    =>
                        val lambda = submitLamb(lamb)
                        toRemoteEvent = (ref, engine) => lambda(Array(ref, engine))
                    case CurrentToRemoteModifier =>
                        val lambda = submitLamb(lamb)
                        toRemote = (ref, engine) => lambda(Array(ref, engine))
                    case RemoteToCurrentEvent    =>
                        val lambda = submitLamb(lamb)
                        fromRemoteEvent = (ref, engine) => lambda(Array(ref, engine))
                    case RemoteToCurrentModifier =>
                        val lambda = submitLamb(lamb)
                        fromRemote = (ref, engine) => lambda(Array(ref, engine))
                })

                @inline
                private def submitLamb(lambda: LambdaExpression): Array[Any] => AnyRef = {
                    lambdas.submitLambda(lambda.block.sourceCode, classOf[AnyRef], classOf[Engine])
                }
            }
        }
        new SimpleValueContract(sync, Some(modifier))
    }

    private def disabledMethodDesc(signature: MethodSignature,
                                   hideMessage: Option[String],
                                   ssd: SyncStructureDescription[_ <: AnyRef]): MethodContractDescriptor = {
        val clazz      = ssd.clazz
        val javaMethod = clazz.getMethod(signature.methodName, signature.params.map(p => findClass(p.tpe)): _*)
        val desc       = new MethodDesc(javaMethod, ssd)
        val builder    = new GenericRMIRulesAgreementBuilder()
        MethodContractDescriptorImpl(desc, None, None, Array.empty, hideMessage, false, builder)
    }

    private def findClass(name: String): Class[_] = {
        importedClasses.getOrElse(name, Class.forName(name))
    }

    private def importClass(name: String): Unit = {
        val cl = Class.forName(name)
        importedClasses.put(cl.getSimpleName, cl)
    }

    private implicit def toClass(name: String): Class[_] = findClass(name)

    private implicit def toClassTag[X](clazz: Class[_]): ClassTag[X] = ClassTag(clazz)

}
