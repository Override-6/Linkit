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
import fr.linkit.engine.local.generation.{AbstractValueScope, SimpleJavaClassBlueprint}

import java.lang.reflect.Modifier

class PuppetWrapperClassGenerator(resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    private val jcbp = new SimpleJavaClassBlueprint(classOf[PuppetWrapperClassGenerator].getResourceAsStream(BPPath), new ClassValueScope(_))

    override def getClass[S](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        val className = clazz.getTypeName
        resources
                .getWrapperClass[S](className)
                .getOrElse({
                    resources.addToQueue(className, genPuppetClassSourceCode(new PuppetDescription(clazz)))
                    resources.compileQueue()
                    resources.getWrapperClass[S](className).get
                })
    }

    override def preGenerateClasses[S](classes: Class[_ <: S]*): Unit = {
        preGenerateDescs(classes.map(new PuppetDescription[S](_)))
    }

    override def preGenerateDescs[S](descriptions: Seq[PuppetDescription[S]]): Unit = {
        descriptions.filter(desc => resources.getWrapperClass(desc.clazz.getName).isEmpty)
                .foreach(desc => {
                    val source = genPuppetClassSourceCode(desc)
                    resources.addToQueue(desc.clazz.getName, source)
                })
        resources.compileQueue()
    }

    override def isClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.getWrapperClass[S](clazz.getName).isDefined
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
