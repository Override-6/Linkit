/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.local.generation.TypeVariableTranslator
import fr.linkit.engine.connection.cache.repo.generation.ScalaWrapperClassBlueprint.MethodValueScope
import fr.linkit.engine.local.generation.cbp.{AbstractClassBlueprint, AbstractValueScope, RootValueScope}

class ScalaWrapperClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](classOf[PuppetWrapperClassGenerator].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp")) {

    override val rootScope: RootValueScope[PuppetDescription[_]] = new RootValueScope[PuppetDescription[_]](blueprint) {
        registerValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        registerValue("CompileTime" ~~> System.currentTimeMillis())
        registerValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        registerValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
        registerValue("TParamsIn" ~> getGenericParamsIn)
        registerValue("TParamsOut" ~> getGenericParamsOut)

        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                .distinctBy(_.methodId)
                .filterNot(desc => {
                    val m = desc.method
                    m.isPrivate || m.isStatic || m.isFinal
                })
                .foreach(action)
        })

    }

    private def getGenericParamsIn(desc: PuppetDescription[_]): String = {
        val result = TypeVariableTranslator.toJavaDeclaration(desc.clazz.getTypeParameters)
        if (result.isEmpty)
            ""
        else s"[$result]"
    }

    private def getGenericParamsOut(desc: PuppetDescription[_]): String = {
        val result = desc
            .clazz
            .getTypeParameters
            .map(_.getName)
            .mkString(", ")
        if (result.isEmpty)
            ""
        else s"[$result]"
    }
}

object ScalaWrapperClassBlueprint {

    case class MethodValueScope(blueprint: String, pos: Int)
        extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        registerValue("ReturnType" ~> (getReturnType))
        registerValue("DefaultReturnType" ~> (_.getDefaultTypeReturnValue))
        registerValue("GenericTypesIn" ~> getGenericParamsIn)
        registerValue("GenericTypesOut" ~> getGenericParamsOut)
        registerValue("MethodName" ~> (d => {
            d.method.name.toString
        }))
        //registerValue("MethodExceptions" ~> getMethodThrows)
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        registerValue("ParamsIn" ~> getParametersIn)
        registerValue("ParamsOut" ~> getParametersOut)

        private def getReturnType(desc: MethodDescription): String = {
            val method = desc.method
            val typeString = method.returnType.toString
            if (typeString == method.owner.name + ".this.type")
                "this.type"
            else typeString
        }

        private def getGenericParamsIn(desc: MethodDescription): String = {
            val tParams = desc.method.typeParams
            if (tParams.isEmpty)
                return ""
            tParams
                .map(s => s.name + s.typeSignature.toString)
                .mkString("[", ", ", "]")
        }

        def getGenericParamsOut(desc: MethodDescription): String = {
            val tParams = desc.method.typeParams.map(_.name)
            if (tParams.isEmpty)
                return ""
            tParams.mkString("[", ", ", "]")
        }

        private def getParametersIn(methodDesc: MethodDescription): String = {
            var i = 0

            def n = {
                i += 1
                i
            }

            val v = methodDesc
                .method
                .paramLists
                .map(_
                    .map(s => s"arg${n}: ${s.typeSignature.toString}")
                    .mkString("(", ", ", ")"))
                .mkString("")
            v
        }

        private def getParametersOut(methodDesc: MethodDescription): String = {
            var i = 0

            def n = {
                i += 1
                i
            }

            val v = methodDesc
                .method
                .paramLists
                .map(_
                    .map(s => s"arg${n}")
                    .mkString("(", ", ", ")"))
                .mkString("")
            v
        }

    }

}
