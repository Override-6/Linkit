/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.parsers

import fr.linkit.engine.internal.language.bhv.parsers.BehaviorLanguageTokens._

import scala.util.parsing.combinator.RegexParsers

object BehaviorLanguageParser extends RegexParsers {

    override val whiteSpace = "[ \t\r\f\n]+".r

    override def skipWhitespace = true

    /////////IMPORT PARSERS
    private val importExp = rep("import ".r ~> ("[^\\s]+".r ^^ ImportToken))

    ////////CLASS PARSERS
    private final val ReturnValue = "returnvalue"
    private val clazz = {
        val classHead       = "describe" ~>
                ((("static class" ^^^ true | "class" ^^^ false) ~ ("[^\\s]+".r ^^ ClassReference))
                        ^^ { case isStatic ~ ref => ClassDescriptionHead(isStatic, ref) }) <~ "{"
        val methodSignature = "(\\w+\\()([\\w,.\\s\\[\\]]+)\\)".r ^^ (s => MethodSignature(s, parseSyncParams(s)))
        val disableMethod   = ("disable" | "disable" ~ "method") ~> methodSignature ^^ DisabledMethodDescription
        val enableMethod    = {
            //head parsing, example 'enable method example(String, synchronized MyObject, Int[])`
            (((("enable" | "enable" ~ "method") ~> methodSignature) <~ "{") ~
                    //parsing the method contract body,
                    // example:
                    // modify 1 2 returnvalue {
                    //     remote_to_current -> ${/*some scala code here*/}
                    //     current_to_remote_event -> ${/*some scala code here*/}
                    // }
                    // synchronize returnvalue

                    //parsing the 'modify' clojure
                    rep(("modify" ~> (rep("\\d+".r | once(ReturnValue)) ^^ (_.filter(_ != "&"))) <~ "{")
                            ~ rep(
                        scalaCodeIntegration("remote_to_current", RemoteToCurrentModifier)
                                | scalaCodeIntegration("current_to_remote", CurrentToRemoteModifier)
                                | scalaCodeIntegration("current_to_remote_event", CurrentToRemoteEvent)
                                | scalaCodeIntegration("remote_to_current_event", RemoteToCurrentEvent)
                    ) <~ "}" ^^ {
                        case concernedComps ~ lambdas => MethodComponentsModifier(
                            concernedComps.filter(_ != ReturnValue).map(i => (i.toInt, lambdas)).toMap,
                            if (concernedComps.contains(ReturnValue)) lambdas else Seq())
                    })
                    // parsing the 'synchronize returnvalue' declaration (which is optional)
                    ~ ("" ^^^ false | ("synchronize" ~ ReturnValue ^^^ true))) ^^ {
                case head ~ modifiers ~ syncReturnValue => EnabledMethodDescription(head, syncReturnValue, modifiers)
            }
        }
        val hideMethod = ("hide" | "hide" ~ "method") ~> methodSignature ~ "REGEX LITTERAL PATTERN".r
        val field    = {
            ("synchronize" ^^^true | "!synchronize" ^^^false) ~ "[^./\\\\*!:;]".r ^^
                    {case sync ~ name => FieldDescription(sync, name)}
        }

    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        once(name ~ "->" ~> ("\\$\\{.*}".r ^^ (LambdaExpression(_, kind))))
    }

    private val synchronisePattern = "\\s*synchronize\\s*.*".r.pattern

    private def parseSyncParams(signature: String): Seq[Int] = {
        signature.split(",")
                .zipWithIndex
                .filter { case (str, _) => synchronisePattern.matcher(str).matches() }
                .map(_._2)
    }

    private def once[P](parser: Parser[P]): Parser[P] = {
        repNM(1, 1, parser) ^^ (_.head)
    }

}
