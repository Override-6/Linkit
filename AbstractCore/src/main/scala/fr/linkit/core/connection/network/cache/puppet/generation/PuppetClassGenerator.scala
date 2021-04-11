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

package fr.linkit.core.connection.network.cache.puppet.generation

import fr.linkit.core.connection.network.cache.puppet.{PuppetClassFields, PuppetObject, Puppeteer, Shared}
import javassist._

import java.lang.reflect.{Method, Modifier}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object PuppetClassGenerator {

    val GeneratedClassesPrefix: String = "fr.linkit.core.generated.puppet"
    private val PrivateFinal = Modifier.PRIVATE & Modifier.FINAL

    private val generatedClasses = new mutable.HashMap[Class[_], Class[_ <: PuppetObject]]()

    def getOrGenerate[S <: Serializable](clazz: Class[_ <: S]): Class[S with PuppetObject] = {
        generatedClasses.getOrElseUpdate(clazz, genPuppetClass(clazz)).asInstanceOf[Class[S with PuppetObject]]
    }

    private def genPuppetClass[S <: Serializable](clazz: Class[S]): Class[S with PuppetObject] = {
        implicit val classPool: ClassPool = ClassPool.getDefault
        val desc     = PuppetClassFields.ofRef(clazz)
        val ctPuppet = classPool.makeClass(GeneratedClassesPrefix + s".Puppet${clazz.getSimpleName}")

        ctPuppet.setSuperclass(classPool.get(clazz.getName))
        ctPuppet.addInterface(classPool.get(classOf[PuppetObject].getName))

        putField(ctPuppet, "puppeteer", classOf[Puppeteer[S]])
        val constructor = new CtConstructor(Array(toCtClass(classOf[Puppeteer[S]]), toCtClass(clazz)), ctPuppet)
        constructor.setBody(
            """
              |super($2);
              |this.puppeteer = $1;
              |this.puppeteer.init(this);
              |""".stripMargin)
        ctPuppet.addConstructor(constructor)

        desc.foreachSharedMethods(putMethod(_, ctPuppet))
        ctPuppet.toClass.asInstanceOf[Class[S with PuppetObject]]
    }

    private def putMethod(method: Method, ctPuppet: CtClass)(implicit classPool: ClassPool): Unit = {
        val name       = method.getName
        val isConstant = method.getAnnotation[Shared](classOf[Shared]).constant()
        val returnType = method.getReturnType
        val ctMethod   = new CtMethod(toCtClass(returnType), name, toCtClasses(method.getParameterTypes), ctPuppet)

        val returnsVoid = Array(classOf[Unit], classOf[Nothing], Void.TYPE).contains(returnType)
        val parameters  = ListBuffer[String]()

        for (i <- 0 to method.getParameterCount)
            parameters += s"$$$i"

        val paramsInput = parameters.mkString(",")

        val invokeLine = {
            val invokeMethodSuffix = if (returnsVoid) "" else "AndReturn"
            val s = '\"'
            s"puppeteer.sendInvoke${invokeMethodSuffix}($s${name}$s, $paramsInput);"
        }

        if (isConstant) {
            if (returnsVoid)
                throw new InvalidPuppetDefException("Shared method defined as constant getter returns void.")

            val varName = s"${name}_0"
            putField(ctPuppet, varName, returnType, Modifier.PRIVATE)
            ctMethod.insertBefore(
                s"""
                   |if ($varName == null) {
                   |   $varName = $invokeLine;
                   |}
                   |return $varName;
                   |""".stripMargin)
        } else {
            ctMethod.insertBefore(invokeLine)
        }
        ctPuppet.addMethod(ctMethod)
    }

    private def putField(owner: CtClass, name: String, fieldType: Class[_], modifiers: Int = PrivateFinal)(implicit pool: ClassPool): Unit = {
        val puppeteerField = new CtField(toCtClass(fieldType), name, owner)
        puppeteerField.setModifiers(modifiers)
        owner.addField(puppeteerField)
    }

    private def toCtClass(clazz: Class[_])(implicit pool: ClassPool): CtClass = {
        pool.get(clazz.getName)
    }

    private def toCtClasses(classes: Array[Class[_]])(implicit pool: ClassPool): Array[CtClass] = {
        classes.map(toCtClass)
    }

}
