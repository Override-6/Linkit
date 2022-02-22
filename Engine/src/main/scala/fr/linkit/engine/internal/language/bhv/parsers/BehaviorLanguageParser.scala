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

    override val whiteSpace = "\\s+".r

    override def skipWhitespace = true

    /////////IMPORT PARSERS
    private val importExp = "import " ~> ("[^\\s]+".r ^^ ImportToken)

    ////////CLASS PARSERS
    private final val ReturnValue      = "returnvalue"
    private final val ForcedSync       = SynchronizeState(true, true)
    private final val NotForcedNotSync = SynchronizeState(true, false)
    private       val clazz            = {
        val syncOrNot = "synchronize" ^^^ ForcedSync | "!synchronize".r ^^^ SynchronizeState(true, false)
        val reference = ("as" ~> ("@\\w+".r ^^ (x => Some(ExternalReference(x))) | "\\w+".r ^^ (x => Some(InternalReference(x))))) ||| "" ^^^ None

        val classHead        = "describe" ~> ((("static class" ^^^ true | "class" ^^^ false) ~ "[^\\s]+".r ~ reference)
                ^^ { case isStatic ~ ref ~ referent => ClassDescriptionHead(isStatic, ref, referent) })
        val methodSignature  = "\\w+\\(".r ~ "([\\w,.\\s\\[\\]]+)\\)".r ^^ { case name ~ signature =>
            MethodSignature(name.dropRight(1), '(' + signature, parseSyncParams(signature))
        }
        val disableMethod    = ("disable" ||| "disable" ~ "method") ~> methodSignature ^^ (x => DisabledMethodDescription(Some(x)))
        val enableMethodHead = ("enable" ||| "enable" ~ "method") ~> methodSignature
        val enableMethodCore = {
            val param = "\\d+".r | ReturnValue

            def and: Parser[List[String]] = (param ~ "&" ~ and) ^^ { case hd ~ _ ~ tail => hd :: tail } | param ^^ (List(_))

            reference ~ ("{" ~>
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
                    ~ ("" ^^^ NotForcedNotSync ||| syncOrNot <~ ReturnValue)

                    <~ "}" ||| ("" ^^^ new ~(List(), NotForcedNotSync)))
        }
        val enableMethod     = {
            //head parsing, example 'enable method example(String, synchronized MyObject, Int[])`
            enableMethodHead ~ enableMethodCore ^^ {
                case signature ~ (referent ~ (modifiers ~ syncReturnValue)) =>
                    EnabledMethodDescription(referent, Some(signature), syncReturnValue, modifiers)
            }
        }
        val hideMethodHead   = ("hide" ||| "hide" ~ "method") ~> methodSignature
        val hideMethodCore   = "\"([^\"\\\\]|\\\\.)*\"".r ^^ (_.drop(1).dropRight(1))
        val hideMethod       = hideMethodHead ~ hideMethodCore ^^ { case signature ~ errorMessage => HiddenMethodDescription(Some(signature), errorMessage) }
        val foreachMethod    = {
            "foreach" ~> "method" ~>
                    ("enable" ~> enableMethodCore ^^ {
                        case referent ~ (modifiers ~ syncReturnValue) => EnabledMethodDescription(referent, None, syncReturnValue, modifiers)
                    }
                            | "disable" ^^^ DisabledMethodDescription(None)
                            | "hide" ~> hideMethodCore ^^ (HiddenMethodDescription(None, _)))
        }
        val field            = syncOrNot ~ "[^./\\\\*!:;{}()\\[\\]]+".r ^^ {
            case sync ~ name => FieldDescription(sync, Option(name))
        }
        val foreachField     = "foreach" ~> "field" ~> syncOrNot ^^ (FieldDescription(_, None))
        (classHead <~ "{") ~ rep(field | disableMethod | enableMethod | hideMethod | foreachMethod | foreachField) <~ "}" ^^ {
            case head ~ content => ClassDescription(head, keep[MethodDescription](content), keep[FieldDescription](content))
        }
    }

    private def keep[X: ClassTag](s: Seq[BHVLangToken]): Seq[X] = {
        val cl = classTag[X].runtimeClass
        s.filter(o => cl.isAssignableFrom(o.getClass)).map(_.asInstanceOf[X])
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        name ~ "->" ~> ("\\$\\{.*}".r ^^ (exp => LambdaExpression(exp.drop(2).dropRight(1), kind)))
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
