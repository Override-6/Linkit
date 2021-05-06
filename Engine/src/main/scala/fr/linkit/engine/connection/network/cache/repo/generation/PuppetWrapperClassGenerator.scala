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

package fr.linkit.engine.connection.network.cache.repo.generation

import fr.linkit.api.connection.network.cache.repo.PuppetDescription.MethodDescription
import fr.linkit.api.connection.network.cache.repo.generation.PuppetWrapperGenerator
import fr.linkit.api.connection.network.cache.repo.{PuppetDescription, PuppetWrapper}
import fr.linkit.api.local.generation.ValueIterator
import fr.linkit.engine.connection.network.cache.repo.generation.PuppetWrapperClassGenerator.{BPPath, ClassValueScope}
import fr.linkit.engine.local.generation.{AbstractValueScope, SimpleJavaClassBlueprint}

class PuppetWrapperClassGenerator(resources: WrappersClassResource) extends PuppetWrapperGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    private val jcbp = new SimpleJavaClassBlueprint(classOf[PuppetWrapperClassGenerator].getResourceAsStream(BPPath), new ClassValueScope(_))

    override def getClass[S <: Serializable](clazz: Class[S]): Class[S with PuppetWrapper[S]] = {
        if (clazz.isInterface)
            throw new InvalidPuppetDefException("Provided class is an interface.")
        val className = clazz.getName
        resources.getWrapperClass[S](className)
            .getOrElse({
                resources.addToQueue(className, genPuppetClassSourceCode(new PuppetDescription(clazz)))
                resources.compileQueue()
                resources.getWrapperClass[S](className).get
            })
    }

    override def preGenerateClasses[S <: Serializable](classes: Class[_ <: S]*): Unit = {
        preGenerateDescs(classes.map(new PuppetDescription[S](_)))
    }

    override def preGenerateDescs[S <: Serializable](descriptions: Seq[PuppetDescription[S]]): Unit = {
        descriptions.foreach(desc => {
            val source = genPuppetClassSourceCode(desc)
            resources.addToQueue(desc.clazz.getName, source)
        })
        resources.compileQueue()
    }

    override def isClassGenerated[S](clazz: Class[S]): Boolean = {
        resources.getWrapperClass[S with Serializable](clazz.getName).isDefined
    }

    private def genPuppetClassSourceCode[S <: Serializable](description: PuppetDescription[S]): String = {
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

        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => desc.foreachMethods(action))
    }

    case class MethodValueScope(blueprint: String, pos: Int)
        extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        registerValue("ReturnType" ~> (_.method.getReturnType.getName))
        registerValue("MethodName" ~> (_.method.getName))
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getReplacedReturnValue.getOrElse("null")))
        registerValue("ParamsIn" ~> getParametersIn)
        registerValue("ParamsOut" ~> getParametersOut)

        private def getParametersIn(methodDesc: MethodDescription): String = {
            var count = 0
            val sb    = new StringBuilder
            methodDesc.method.getParameterTypes.foreach(clazz => {
                val typeName = clazz.getTypeName
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
