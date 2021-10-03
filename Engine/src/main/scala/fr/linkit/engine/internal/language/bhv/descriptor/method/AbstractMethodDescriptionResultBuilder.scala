/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.language.bhv.descriptor.method

import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule
import fr.linkit.engine.gnom.cache.sync.behavior.AnnotationBasedMemberBehaviorFactory.DefaultMethodControl
import fr.linkit.engine.internal.language.bhv.ParserAssertions
import fr.linkit.engine.internal.language.bhv.descriptor.clazz.ClassDescriptionResultBuilder

import java.util.Scanner

abstract class AbstractMethodDescriptionResultBuilder(protected val scanner: Scanner, classBuilder: ClassDescriptionResultBuilder) {

    protected var behaviorRule   : BasicInvocationRule = DefaultMethodControl.value()
    protected var syncReturnValue: Boolean             = false

    protected def launchParsing(): Unit = {
        parseType()
        if (scanner.hasNext("\\{")) {
            scanner.next() //will be "{"
            parseFurtherInformation()
        }
    }

    private def parseFurtherInformation(): Unit = {
        var word = scanner.next()
        while (word != "}") {
            word match {
                case "returnvalue"                      => parseReturnValue()
                case category if scanner.hasNext("\\{") => parseCategory(category)
                case other                              => throw new MethodBehaviorDescriptionException(s"Unknown value '$other'.")
            }
            word = scanner.next()
        }
    }

    protected def parseCategory(name: String): Unit

    private def parseReturnValue(): Unit = {
        if (scanner.next() != "->")
            throw new MethodBehaviorSyntaxDescriptionException("Expected '->' after 'returnvalue' description.")
        val word = scanner.next();
        ParserAssertions.assertEOL(scanner)
        word match {
            case "enable"  => syncReturnValue = true
            case "disable" => syncReturnValue = false
            case other     => throw new MethodBehaviorDescriptionException(s"Wrong: 'returnvalue -> $other' Expected 'enable' or 'disable' but found '$other' during method return value description.")
        }
    }

    private def parseType(): Unit = {
        val behaviorName = scanner.next()
        try {
            behaviorRule = BasicInvocationRule.valueOf(behaviorName.toUpperCase)
        } catch {
            case e: IllegalArgumentException =>
                val otherResult = classBuilder.getMethodResult(behaviorName).getOrElse {
                    throw new MethodBehaviorDescriptionException(s"Unknown behavior : $behaviorName", e)
                }
                syncReturnValue = otherResult.syncReturnValue
                behaviorRule = otherResult.rule
                baseCurrentOn(otherResult)
        }
    }

    def baseCurrentOn(result: MethodBehaviorDescriptionResult): Unit

    def result(): MethodBehaviorDescriptionResult
}
