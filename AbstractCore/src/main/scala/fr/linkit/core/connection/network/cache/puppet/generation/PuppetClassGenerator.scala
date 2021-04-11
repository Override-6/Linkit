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
import javax.tools.JavaCompiler
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object PuppetClassGenerator {

    val GeneratedClassesPrefix: String = "fr.linkit.core.generated.puppet"
    private val PrivateFinal = Modifier.PRIVATE & Modifier.FINAL
    private val RootPool     = {
        val pool = ClassPool.getDefault
        pool.appendClassPath(getClass.getProtectionDomain.getCodeSource.getLocation.getPath)
        pool
    }

    private val generatedClasses = new mutable.HashMap[Class[_], Class[_ <: PuppetObject]]()

    def getOrGenerate[S <: Serializable](clazz: Class[_ <: S]): Class[S with PuppetObject] = {
        generatedClasses.getOrElseUpdate(clazz, genPuppetClass(clazz)).asInstanceOf[Class[S with PuppetObject]]
    }

    private def genPuppetClass[S <: Serializable](clazz: Class[S]): Class[S with PuppetObject] = {
        implicit val classPool: ClassPool = new ClassPool(RootPool)

        val desc     = PuppetClassFields.ofRef(clazz)
        val puppetClassName = s"Puppet${clazz.getSimpleName}"
        val ctPuppet = classPool.makeClass(GeneratedClassesPrefix + s".$puppetClassName")

        ctPuppet.setSuperclass(toCtClass(clazz))
        ctPuppet.addInterface(toCtClass(classOf[PuppetObject]))

        putField(ctPuppet, "puppeteer", classOf[Puppeteer[S]])
        val constructor = new CtConstructor(Array(toCtClass(classOf[Puppeteer[S]]), toCtClass(clazz)), ctPuppet)
        constructor.setBody(
            """{
              |    super($2);
              |    this.puppeteer = $1;
              |    this.puppeteer.init(this);
              |}""".stripMargin)
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

        for (i <- 0 until method.getParameterCount)
            parameters += s"$$$i"

        val paramsInput = s"new Object[]{${parameters.mkString(",")}}"

        val invokeLine = {
            val invokeMethodSuffix = if (returnsVoid) "" else "AndReturn"
            val s                  = '\"'
            s"(${returnType.getName}) puppeteer.sendInvoke${invokeMethodSuffix}($s${name}$s, $paramsInput)"
        }

        if (isConstant) {
            if (returnsVoid)
                throw new InvalidPuppetDefException("Shared method defined as constant getter returns void.")

            val varName = s"${name}_0"
            println(s"varName = ${varName}")
            ctPuppet.debugWriteFile()
            putField(ctPuppet, varName, returnType, Modifier.PRIVATE)
            val body = s"""{
                          |    if ($varName == null) {
                          |        $varName = $invokeLine;
                          |    }
                          |    return $varName;
                          |}""".stripMargin
            println(s"body = ${body}")
            ctMethod.setBody(body)
        } else {
            ctMethod.setBody(s"{$invokeLine;}")
        }
        ctPuppet.addMethod(ctMethod)
    }

    private def putField(owner: CtClass, name: String, fieldType: Class[_], modifiers: Int = PrivateFinal)(implicit pool: ClassPool): Unit = {
        val puppeteerField = new CtField(toCtClass(fieldType), name, owner)
        puppeteerField.setModifiers(modifiers)
        owner.addField(puppeteerField)
    }

    private def toCtClass(clazz: Class[_])(implicit pool: ClassPool): CtClass = {
        try {
            pool.get(clazz.getName)
        } catch {
            case e: NotFoundException =>
                RootPool.appendClassPath(clazz.getProtectionDomain.getCodeSource.getLocation.getPath)
                /*
                 * Do not use recursion because, if NotFoundException persists after class path is appended to the pool,
                 * it would throw a StackOverflowError.
                 */
                pool.get(clazz.getName)
        }
    }

    private def toCtClasses(classes: Array[Class[_]])(implicit pool: ClassPool): Array[CtClass] = {
        classes.map(toCtClass)
    }

}
