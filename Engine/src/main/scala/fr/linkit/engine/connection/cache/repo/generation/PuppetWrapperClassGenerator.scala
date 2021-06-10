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

import java.lang.reflect.{Modifier, Type}

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.connection.cache.repo.generation.PuppetWrapperGenerator
import fr.linkit.api.connection.cache.repo.{InvalidPuppetDefException, PuppetWrapper}
import fr.linkit.api.local.generation.{CompilerCenter, CompilerType, TypeVariableTranslator}
import fr.linkit.engine.connection.cache.repo.generation.PuppetWrapperClassGenerator.ClassValueScope
import fr.linkit.engine.local.generation.access.CommonCompilerTypes
import fr.linkit.engine.local.generation.cbp.{AbstractValueScope, SimpleClassBlueprint}

class PuppetWrapperClassGenerator(center: CompilerCenter, resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    private val scbp = new SimpleClassBlueprint(classOf[PuppetWrapperClassGenerator].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp"), new ClassValueScope(_))

    override def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        getClass[S](PuppetDescription(clazz))
    }

    override def getClass[S](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        var loader = clazz.getClassLoader
        if (loader == null)
            loader = classOf[PuppetWrapperClassGenerator].getClassLoader //Use the Application classloader
        resources
            .findWrapperClass[S](clazz, loader)
            .getOrElse {
                val (sourceCode, compilerType) = genPuppetClassSourceCode(desc)
                resources.addToQueue(clazz, sourceCode, compilerType)
                resources.compileQueue(loader)
                resources.findWrapperClass[S](clazz, loader).get
            }
    }

    override def preGenerateClasses[S](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit = {
        preGenerateDescs(defaultLoader, classes.map(PuppetDescription[S]))
    }

    override def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit = {
        descriptions
            .filter(desc => resources.findWrapperClass(desc.clazz, desc.clazz.getClassLoader).isEmpty)
            .foreach(desc => {
                val (source, compileType) = genPuppetClassSourceCode(desc)
                resources.addToQueue(desc.clazz, source, compileType)
            })
        resources.compileQueue(defaultLoader)
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    override def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean = {
        resources.findWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    private def genPuppetClassSourceCode[S](description: PuppetDescription[S]): (String, CompilerType) = {
        (scbp.toClassSource(description), CommonCompilerTypes.Scalac)
    }
}

object PuppetWrapperClassGenerator {

    class ClassValueScope(blueprint: String)
        extends AbstractValueScope[PuppetDescription[_]]("CLASS", 0, blueprint) {

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
            val v = TypeVariableTranslator
                .toScalaDeclaration(desc
                    .method
                    .getGenericReturnType)
            v
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
