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

package fr.linkit.engine.local.parsing.bhv.descriptor.clazz

import fr.linkit.engine.local.parsing.bhv.descriptor.FieldDescriptionResult
import fr.linkit.engine.local.parsing.bhv.descriptor.method.MethodDescriptionResult
import fr.linkit.engine.local.parsing.bhv.{BehaviorFileException, BehaviorFileSyntaxException}

import java.util.Scanner
import scala.collection.mutable

class ClassDescriptionResultBuilder(scanner: Scanner, clazz: Class[_]) {

    private val className                                      = clazz.getName
    private var defaultFieldBehavior : FieldDescriptionResult  = _
    private var defaultMethodBehavior: MethodDescriptionResult = _

    private val fieldMap  = mutable.HashMap.empty[String, FieldDescriptionResult]
    private val methodMap = mutable.HashMap.empty[String, MethodDescriptionResult]

    launchParsing()

    private def launchParsing(): Unit = {
        var word = scanner.next()
        while (word != "}") {
            word match {
                case "forall"  => parseForall()
                case "enable"  => parseEnabledMember()
                case "disable" => parseDisabledMember()
            }
            word = scanner.next()
        }
    }

    private def parseForall(): Unit = {
        @inline
        def fail(memberName: String): Nothing = {
            throw new BehaviorFileException(s"'forall $memberName' already defined for class $className description.")
        }

        scanner.next() match {
            case "field"  =>
                if (defaultFieldBehavior != null)
                    fail("field")
                defaultFieldBehavior = parseField()
            case "method" =>
                if (defaultMethodBehavior != null)
                    fail("method")
                defaultMethodBehavior = ???
        }
    }

    private def parseField(): FieldDescriptionResult = {
        val word  = scanner.next()
        val isEOL = scanner.nextLine().isEmpty
        word match {
            case "disable" =>
                if (!isEOL)
                    throw new BehaviorFileSyntaxException("Disabled fields don't needs more instruction (end of line not reached)")
                new FieldDescriptionResult(isEnabled = false)
            case "enabled" =>
                if (!isEOL)
                    throw new UnsupportedOperationException("Fields can either be enabled or disabled, further enabled description is not yet supported.")
                new FieldDescriptionResult(isEnabled = true)
        }
    }

    private def parseEnabledMember(): Unit = {

    }

    private def parseDisabledMember(): Unit = {
        val word       = scanner.next()
        word match {
            case "field"  =>
                val memberName = scanner.next()
                if (scanner.nextLine().nonEmpty)
                    throw new UnsupportedOperationException("Fields can either be enabled or disabled, further enabled description is not yet supported.")
                fieldMap.put(memberName, FieldDescriptionResult.Disabled)
            case "method" =>
                val memberName = scanner.next()
                methodMap.put(memberName, MethodDescriptionResult.Disabled)
            case methodName   =>
                methodMap.put(methodName, MethodDescriptionResult.Disabled)
        }
    }

    def result(): ClassDescriptionResult = {
        null
    }
}
