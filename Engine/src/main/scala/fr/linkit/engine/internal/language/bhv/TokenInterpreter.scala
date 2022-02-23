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

import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.ContractDescriptorDataBuilder
import fr.linkit.engine.internal.language.bhv.compilation.FileIntegratedLambdas
import fr.linkit.engine.internal.language.bhv.parsers.BehaviorLanguageTokens._

import scala.collection.mutable
import scala.reflect.ClassTag

class TokenInterpreter(tokens: List[BHVLangToken], fileName: String,
                       center: CompilerCenter, properties: PropertyClass) {

    private val importedClasses = mutable.HashMap.empty[String, Class[_]]
    private val builder         = new ContractDescriptorDataBuilder {}
    private val lambdas         = new FileIntegratedLambdas(fileName, center)

    private lazy val data: ContractDescriptorData = {
        computeData()
        lambdas.compileLambdas()
        builder.build()
    }

    def getData: ContractDescriptorData = {
        data
    }

    private def computeData(): Unit = {
        tokens.foreach {
            case ImportToken(name)                       =>
                importClass(name)
            case ClassDescription(head, methods, fields) =>
                computeClassDesc(head, methods, fields)
        }
    }

    private def computeClassDesc(head: ClassDescriptionHead, methods: Seq[MethodDescription], fields: Seq[FieldDescription]): Unit = {
        val static     = head.static
        val className  = head.className
        val desc       = if (static) SyncStaticsDescription(className) else SyncObjectDescription(className)
        val descriptor = new builder.ClassDescriptor[Nothing]()(desc) {}
        methods.foreach {
            case DisabledMethodDescription(None)                                           =>
                descriptor.disable.allMethods()
            case DisabledMethodDescription(Some(signature))                                =>

                descriptor.disable.method(signature.methodName)(signature.params.map(getClass): _*)
            case EnabledMethodDescription(referent, signature, syncReturnValue, modifiers) =>
                signature
            case HiddenMethodDescription(signature, errorMessage)                          =>
            case _                                                                         =>
        }
        builder.describe(descriptor)
    }


    private def getClass(name: String): Class[_] = {
        importedClasses.getOrElse(name, Class.forName(name))
    }

    private def importClass(name: String): Unit = {
        val cl = Class.forName(name)
        importedClasses.put(cl.getSimpleName, cl)
    }

    private implicit def toClass(name: String): Class[_] = getClass(name)

    private implicit def toClassTag[X](clazz: Class[_]): ClassTag[X] = ClassTag(clazz)

}
