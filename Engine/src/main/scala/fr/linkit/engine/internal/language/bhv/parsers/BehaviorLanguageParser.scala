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

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.parsers.BehaviorLanguageTokens._
import fr.linkit.engine.internal.language.bhv.parsers.ParserErrorMessageHelper.makeErrorMessage

import scala.reflect.{ClassTag, classTag}
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageParser extends RegexParsers {

    override val whiteSpace = "(\\s+)|(//[^\\n]+)|(/\\*((.|\\n)+)\\*/)".r
    override def skipWhitespace = true

    /////////IMPORT PARSERS
    private val importExp = "import " ~> ("[^\\s]+".r ^^ ImportToken)

    ////////CLASS PARSERS
    private final val ReturnValue = "returnvalue"
    private final val ForcedSync = SynchronizeState(true, true)
    private final val NotForcedNotSync = SynchronizeState(false, false)
    private       val clazz       = {
        val classHead        = "describe" ~>
                ((("static class" ^^^ true | "class" ^^^ false) ~ ("[^\\s]+".r ^^ ClassReference))
                        ^^ { case isStatic ~ ref => ClassDescriptionHead(isStatic, ref) })
        val methodSignature  = "\\w+\\(".r ~ "([\\w,.\\s\\[\\]]+)\\)".r ^^ { case name ~ signature =>
            MethodSignature(name.dropRight(1), '(' + signature, parseSyncParams(signature))
        }
        val disableMethod    = ("disable" | "disable" ~ "method") ~> methodSignature ^^ DisabledMethodDescription
        val enableMethodHead = ("enable" ||| "enable" ~ "method") ~> methodSignature
        val enableMethod     = {
            val param = "\\d+".r | ReturnValue

            def and: Parser[List[String]] = (param ~ "&" ~ and) ^^ { case hd ~ _ ~ tail => hd :: tail } | param ^^ (List(_))
            //head parsing, example 'enable method example(String, synchronized MyObject, Int[])`
            enableMethodHead ~
                    ((("as" ~> "\\w+".r ^^ (x => Some(MethodReference(x)))) ||| "" ^^^ None) ~ ("{" ~>
                            //parsing the method contract body,
                            // example:
                            // modify 1 & 2 & returnvalue {
                            //     remote_to_current -> ${/*some scala code here*/}
                            //     current_to_remote_event -> ${/*some scala code here*/}
                            // }
                            // synchronize returnvalue

                            //parsing the 'modify' clojure
                            rep(("modify" ~> and <~ "{")
                                    ~ rep(
                                scalaCodeIntegration("distant_to_current", RemoteToCurrentModifier)
                                        | scalaCodeIntegration("current_to_distant", CurrentToRemoteModifier)
                                        | scalaCodeIntegration("current_to_distant_event", CurrentToRemoteEvent)
                                        | scalaCodeIntegration("distant_to_current_event", RemoteToCurrentEvent)
                            ) <~ "}" ^^ {
                                case concernedComps ~ lambdas => MethodComponentsModifier(
                                    concernedComps.filter(_ != ReturnValue).map(i => (i.toInt, lambdas)).toMap,
                                    if (concernedComps.contains(ReturnValue)) lambdas else Seq())
                            })
                            // parsing the 'synchronize returnvalue' declaration (which is optional)
                            ~ ("" ^^^ NotForcedNotSync ||| ("synchronize|!synchronize".r <~ ReturnValue ^^ (s => SynchronizeState(true, !s.startsWith("!")))))

                            <~ "}"
                            ||| ("" ^^^ new ~(List(), NotForcedNotSync)))) ^^ {
                case head ~ (referent ~ (modifiers ~ syncReturnValue)) =>
                    EnabledMethodDescription(referent, head, syncReturnValue, modifiers)
            }
        }
        val hideMethod       = ("hide" ||| "hide" ~ "method") ~>
                methodSignature ~ ("\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1))) ^^ { case signature ~ errorMessage => HiddenMethodDescription(signature, errorMessage) }
        val field            = {
            ("synchronize" ^^^ ForcedSync | "!synchronize".r ^^^ NotForcedNotSync) ~ "[^./\\\\*!:;{}()\\[\\]]+".r ^^ { case sync ~ name => FieldDescription(sync, name.trim) }
        }
        classHead ~ "{" ~ rep(field | disableMethod | enableMethod | hideMethod) <~ "}" ^^ {
            case head ~ _ ~ content => ClassDescription(head, keep[MethodDescription](content), keep[FieldDescription](content))
        }
    }

    private def keep[X: ClassTag](s: Seq[BHVLangToken]): Seq[X] = {
        val cl = classTag[X].runtimeClass
        s.filter(o => cl.isAssignableFrom(o.getClass)).map(_.asInstanceOf[X])
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        name ~ "->" ~> ("\\$\\{.*}".r ^^ (LambdaExpression(_, kind)))
    }

    private val synchronisePattern = "\\s*synchronize\\s*.*".r.pattern

    private def parseSyncParams(signature: String): Seq[Int] = {
        signature.split(",")
                .zipWithIndex
                .filter { case (str, _) => synchronisePattern.matcher(str).matches() }
                .map(_._2)
    }

    /////////FILE PARSER

    private val fileParser = rep(clazz | importExp)

    def parse(input: CharSequenceReader): Seq[BHVLangToken] = {
        parseAll(fileParser, input) match {
            case Failure(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Error(msg, n)   => throw new BHVLanguageException(makeErrorMessage(msg, "Error", input, n.pos))
            case Success(r, _)   => r
        }
    }

}
