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

package fr.linkit.engine.local.language.bhv.descriptor.method

import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.engine.local.language.bhv.descriptor.clazz.ClassDescriptionResultBuilder

import java.util.Scanner
import scala.collection.mutable.ListBuffer

class ExplicitMethodDescriptionResultBuilder(method: MethodDescription, classBuilder: ClassDescriptionResultBuilder, scanner: Scanner) extends AbstractMethodDescriptionResultBuilder(scanner, classBuilder) {

    private val javaMethod         = method.method
    private val methodName         = javaMethod.getName
    private val className          = methodName.getClass.getName
    private val synchronizedParams = new Array[Boolean](javaMethod.getParameterCount)
    private val params = javaMethod.getParameters

    override protected def parseCategory(name: String): Unit = {
        name match {
            case "args" => parseArgs()
        }
    }

    override def baseCurrentOn(result: MethodBehaviorDescriptionResult): Unit = {
        val params = result.synchronizedParameters
        result.synchronizedParameters.indices.foreach(i => synchronizedParams(i) = params(i))
    }

    override def result(): MethodBehaviorDescriptionResult = {
        new MethodBehaviorDescriptionResult(true, synchronizedParams, syncReturnValue, behaviorRule)
    }

    private def parseArgs(): Unit = {
        if (scanner.next() != "{")
            throwMethodSyntaxException(s"Expected '{' after 'args' category")

        var paramName = scanner.next()
        while (paramName != "}") {
            if (scanner.next() != "->")
                throwMethodSyntaxException(s"Expected '->' after parameter '$paramName' description")

            val i = getParamIndex(paramName)
            scanner.next() match {
                case "enable"  => synchronizedParams(i) = true
                case "disable" => synchronizedParams(i) = false
                case other     => throwMethodSyntaxException(s"Expected 'enable' or 'disable' after parameter description but found $other.")
            }
            paramName = scanner.next()
        }
    }


    private def getParamIndex(paramName: String): Int = {
        val idx = params.indexWhere(_.getName == paramName)
        if (idx == -1) {
            try {
                val i = Integer.parseInt(paramName)
                params(i)
                return i
            } catch {
                case _: NumberFormatException          => throwMethodException(s"Unknown parameter name $paramName.")
                case e: ArrayIndexOutOfBoundsException => throwMethodException(s"Invalid method parameter index $paramName.", e)
            }
        }
        idx
    }

    private def throwMethodSyntaxException(msg: String, cause: Throwable = null): Nothing = {
        throw new MethodBehaviorSyntaxDescriptionException(s"$msg $suffixMessage", cause)
    }

    private def throwMethodException(msg: String, cause: Throwable = null): Nothing = {
        throw new MethodBehaviorDescriptionException(s"$msg $suffixMessage", cause)
    }

    private val suffixMessage: String = s"for method '$methodName' in class '$className'."
    launchParsing()

}
