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

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.typeTag
import java.lang.reflect.{Modifier, Type}

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
                        val mods = desc.method.getModifiers
                        Modifier.isPrivate(mods) || Modifier.isStatic(mods) || Modifier.isFinal(mods)
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

        registerValue("ReturnType" ~> getReturnType)
        registerValue("DefaultReturnType" ~> (_.getDefaultTypeReturnValue))
        registerValue("GenericTypes" ~> getGenericParams)
        registerValue("MethodName" ~> (_.method.getName))
        registerValue("MethodExceptions" ~> getMethodThrows)
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        registerValue("ParamsIn" ~> getParametersIn)
        registerValue("ParamsOut" ~> getParametersOut)


        private def getReturnType(desc: MethodDescription): String = {
            TypeVariableTranslator
                    .toScalaDeclaration(desc
                            .method
                            .getGenericReturnType)
        }

        private def getGenericParams(desc: MethodDescription): String = {
            val tParams = desc.method.getTypeParameters
            if (tParams.isEmpty)
                ""
            else
                s"[${TypeVariableTranslator.toScalaDeclaration(tParams)}]"
        }

        private def getMethodThrows(methodDesc: MethodDescription): String = {
            val exceptions = methodDesc.method.getGenericExceptionTypes
            if (exceptions.isEmpty)
                return ""
            exceptions
                    .map(s => toGenericTypeName(s))
                    .mkString("throws ", ", ", "")
        }

        private def getParametersIn(methodDesc: MethodDescription): String = {
            var count = 0
            val sb    = new StringBuilder
            methodDesc
                    .method
                    .getGenericParameterTypes.foreach(typ => {
                val typeName = TypeVariableTranslator.toScalaDeclaration(typ)
                count += 1
                sb
                        .append("arg")
                        .append(count)
                        .append(": ")
                        .append(typeName)
                        .append(", ")
            })
            val v = sb.toString().dropRight(2) //Remove last ", " string.
            v
        }

        private def getParametersOut(methodDesc: MethodDescription): String = {
            val sb = new StringBuilder
            for (i <- 1 to methodDesc.method.getParameterCount) {
                sb.append("arg")
                        .append(i)
                        .append(", ")
            }
            sb.dropRight(2).toString()
        }

        private def toGenericTypeName(typ: Type): String = {
            //The successor name can be the Package for a top level class or a Class File name for an inner class.
            val name             = typ.getTypeName
            val genericTypeBegin = name.indexOf('<')
            if (genericTypeBegin >= 0) {
                val typeOnly = name.take(genericTypeBegin)
                return typeOnly + name.drop(genericTypeBegin).replace('.', '$')
            }
            name
        }
    }
}
