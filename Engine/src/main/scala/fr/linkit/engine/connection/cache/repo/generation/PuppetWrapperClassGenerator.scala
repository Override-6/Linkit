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

import fr.linkit.api.connection.cache.repo.PuppetDescription.MethodDescription
import fr.linkit.api.connection.cache.repo.generation.PuppetWrapperGenerator
import fr.linkit.api.connection.cache.repo.{PuppetDescription, PuppetWrapper}
import fr.linkit.engine.connection.cache.repo.generation.PuppetWrapperClassGenerator.{BPPath, ClassValueScope}
import fr.linkit.engine.connection.cache.repo.generation.WrappersClassResource.{WrapperPackageName, WrapperPrefixName}
import fr.linkit.engine.local.generation.{AbstractValueScope, SimpleJavaClassBlueprint}

import java.lang.reflect.Modifier

class PuppetWrapperClassGenerator(resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    private val jcbp = new SimpleJavaClassBlueprint(classOf[PuppetWrapperClassGenerator].getResourceAsStream(BPPath), new ClassValueScope(_))

    override def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        getClass[S](PuppetDescription(clazz))
    }

    override def getClass[S](desc: PuppetDescription[S]): Class[S with PuppetWrapper[S]] = {
        val clazz = desc.clazz
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        val loader           = clazz.getClassLoader
        resources
                .getWrapperClass[S](clazz, loader)
                .getOrElse {
                    resources.addToQueue(clazz, genPuppetClassSourceCode(desc))
                    resources.compileQueue(loader)
                    resources.getWrapperClass[S](clazz, loader).get
                }
    }

    override def preGenerateClasses[S](defaultLoader: ClassLoader, classes: Seq[Class[_ <: S]]): Unit = {
        preGenerateDescs(defaultLoader, classes.map(PuppetDescription[S]))
    }

    override def preGenerateDescs[S](defaultLoader: ClassLoader, descriptions: Seq[PuppetDescription[S]]): Unit = {
        descriptions
                .filter(desc => resources.getWrapperClass(desc.clazz, desc.clazz.getClassLoader).isEmpty)
                .foreach(desc => {
                    val source = genPuppetClassSourceCode(desc)
                    resources.addToQueue(desc.clazz, source)
                })
        resources.compileQueue(defaultLoader)
    }

    override def isWrapperClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.getWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    override def isClassGenerated[S <: PuppetWrapper[S]](clazz: Class[S]): Boolean = {
        resources.getWrapperClass[S](clazz, clazz.getClassLoader).isDefined
    }

    private def genPuppetClassSourceCode[S](description: PuppetDescription[S]): String = {
        jcbp.toClassSource(description)
    }
}

object PuppetWrapperClassGenerator {

    val BPPath: String = "/generation/puppet_wrapper_blueprint.jcbp"

    class ClassValueScope(blueprint: String)
            extends AbstractValueScope[PuppetDescription[_]]("CLASS", 0, blueprint) {

        registerValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        registerValue("CompileTime" ~~> System.currentTimeMillis())
        registerValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        registerValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))

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

    case class MethodValueScope(blueprint: String, pos: Int)
            extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        registerValue("ReturnType" ~> (_.method.getGenericReturnType.getTypeName.replaceAll("\\$", ".")))
        registerValue("MethodName" ~> (_.method.getName))
        registerValue("MethodExceptions" ~> getMethodThrows)
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getReplacedReturnValue))
        registerValue("ParamsIn" ~> getParametersIn)
        registerValue("ParamsOut" ~> getParametersOut)

        private def getMethodThrows(methodDesc: MethodDescription): String = {
            val exceptions = methodDesc.method.getGenericExceptionTypes
            if (exceptions.isEmpty)
                return ""
            exceptions.map(_.getTypeName.replaceAll("\\$", ".")).mkString("throws ", ", ", "")
        }

        private def getParametersIn(methodDesc: MethodDescription): String = {
            var count = 0
            val sb    = new StringBuilder
            methodDesc.method.getGenericParameterTypes.foreach(clazz => {
                val typeName = clazz.getTypeName.replaceAll("\\$", ".")
                count += 1
                sb.append(typeName)
                        .append(' ')
                        .append("arg")
                        .append(count)
                        .append(", ")
            })
            sb.toString().dropRight(2) //Remove last ", " string.
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
    }

}
