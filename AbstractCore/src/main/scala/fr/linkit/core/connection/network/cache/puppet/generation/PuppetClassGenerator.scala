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

import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.puppet.AnnotationHelper.Shared
import fr.linkit.core.connection.network.cache.puppet.{PuppetClassFields, PuppetObject}
import fr.linkit.core.local.mapping.ClassMappings

import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.{Files, Path}
import scala.collection.mutable

//FIXME potential Collision if two classes of the same name, but not the same package are generated.
object PuppetClassGenerator {

    val GeneratedClassesPackage: String = "fr.linkit.core.generated.puppet"
    val GeneratedClassesFolder : String = "/generated/"
    val EnqueueingSourcesFolder: String = GeneratedClassesFolder + "/queue/"
    private val path = Path.of(GeneratedClassesFolder)
    if (Files.notExists(path))
        Files.createDirectories(path)
    private val classLoader = new URLClassLoader(Array(path.toUri.toURL))

    private val generatedClasses = new mutable.HashMap[Class[_], Class[_ <: PuppetObject[_]]]()

    def getOrGenerate[S <: Serializable](clazz: Class[_ <: S]): Class[S with PuppetObject[S]] = {
        generatedClasses.getOrElseUpdate(clazz, genPuppetClass(clazz)).asInstanceOf[Class[S with PuppetObject[S]]]
    }

    private def genPuppetClass[S <: Serializable](clazz: Class[_ <: S]): Class[_ <: PuppetObject[_]] = {
        val s          = '\"'
        val classPaths = ClassMappings.getSources.map(source => s"$s${source.getLocation.getPath.drop(1)}$s").mkString(";")
        val sourceCode = genPuppetClassSourceCode[S](clazz)

        val puppetClassName = "Puppet" + clazz.getSimpleName
        val path            = Path.of(EnqueueingSourcesFolder + puppetClassName + ".java").toAbsolutePath
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent)
            Files.createFile(path)
        }
        Files.write(path, sourceCode.getBytes)

        val javacProcess = new ProcessBuilder("javac", "-d", GeneratedClassesFolder, s"-cp", classPaths, path.toString)
        javacProcess.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        javacProcess.redirectError(ProcessBuilder.Redirect.INHERIT)
        javacProcess.directory(new File(GeneratedClassesFolder))

        AppLogger.debug(s"Compiling Puppet class for ${clazz.getSimpleName}...")
        val exitValue = javacProcess.start().waitFor()
        if (exitValue != 0)
            throw new InvalidPuppetDefException(s"Javac rejected compilation of class $puppetClassName, check above error prints for further details.")

        AppLogger.debug(s"Compilation done.")
        val puppetClass = classLoader.loadClass(GeneratedClassesPackage + "." + puppetClassName)
        ClassMappings.putClass(puppetClass)
        puppetClass.asInstanceOf[Class[_ <: PuppetObject[S]]]
    }

    private def genConstantGettersFields(desc: PuppetClassFields): String = {
        val fieldBuilder = new StringBuilder()
        desc.foreachSharedMethods(method => {
            val isConstant = method.getAnnotation(classOf[Shared]).constant()
            if (isConstant) {
                val fieldName = s"${method.getName}_0" //TODO support polymorphism
                val fieldType = method.getReturnType.getName
                fieldBuilder.append(s"private $fieldType $fieldName;")
            }
        })
        fieldBuilder.toString()
    }

    private def genPuppetClassSourceCode[S <: Serializable](clazz: Class[_ <: S]): String = {
        val sourceBuilder = new StringBuilder()
        val desc          = PuppetClassFields.ofClass(clazz)

        val puppetClassSimpleName = s"Puppet${clazz.getSimpleName}"
        val superClassName        = clazz.getCanonicalName
        val puppetClassName       = GeneratedClassesPackage + '.' + puppetClassSimpleName
        val constantGettersFields = genConstantGettersFields(desc)
        val puppeteerType         = s"Puppeteer<$superClassName>"
        sourceBuilder.append(
            s"""
               |package $GeneratedClassesPackage;
               |
               |import fr.linkit.core.connection.network.cache.puppet.Puppeteer;
               |import fr.linkit.core.connection.network.cache.puppet.PuppetObject;
               |import fr.linkit.core.connection.network.cache.puppet.generation.PuppetAlreadyInitialisedException;
               |
               |public class $puppetClassSimpleName extends $superClassName implements PuppetObject<$superClassName> {
               |
               |private transient $puppeteerType puppeteer;
               |$constantGettersFields
               |
               |public $puppetClassSimpleName($puppeteerType puppeteer, $superClassName clone) {
               |    super(clone);
               |    this.puppeteer = puppeteer;
               |    this.puppeteer.init(this);
               |}
               |
               |public void initPuppet(Puppeteer<$superClassName> puppeteer) throws PuppetAlreadyInitialisedException {
               |    if (this.puppeteer != null)
               |        throw new PuppetAlreadyInitialisedException("This puppet is already initialized !");
               |    this.puppeteer = puppeteer;
               |}
               |
               |public boolean canEqual(Object that) {
               |     return that instanceof $puppetClassName;
               |}
               |
               |public int productArity() {
               |     return 0;
               |}
               |
               |public Object productElement(int n) {
               |     return null;
               |}
               |
               |""".stripMargin)

        desc.foreachSharedMethods(method => {
            val name       = method.getName
            var i          = 0
            val parameters = method.getParameterTypes.map(parameterType => {
                i += 1
                parameterType.getName + s" $$$i"
            }).mkString(",")

            val body       = genMethodBody(method)
            val returnType = method.getReturnType.getName
            sourceBuilder.append(
                s"""
                   |public $returnType $name($parameters) {
                   |    $body
                   |}
                   |""".stripMargin)
        })
        sourceBuilder.append('}') // Closing class
            .toString()
    }

    private def genMethodBody(method: Method): String = {
        val isConstant = method.getAnnotation(classOf[Shared]).constant()
        val name       = method.getName

        val returnType  = method.getReturnType
        val returnsVoid = Array(classOf[Unit], classOf[Nothing], Void.TYPE).contains(returnType)

        val parametersNames = for (i <- 1 to method.getParameterCount) yield {
            s"$$$i"
        }

        val paramsInput = s"new Object[]{${parametersNames.mkString(",")}}"

        val invokeLine = {
            val invokeMethodSuffix = if (returnsVoid) "" else "AndReturn"
            val s                  = '\"'
            s"puppeteer.sendInvoke${invokeMethodSuffix}($s${name}$s, $paramsInput)"
        }
        if (isConstant) {
            val varName = s"${name}_0" //TODO support polymorphism.
            s"""
               |if ($varName == null) {
               |    $varName = $invokeLine;
               |}
               |return $varName;
               |""".stripMargin
        } else invokeLine + ';'
    }

}
