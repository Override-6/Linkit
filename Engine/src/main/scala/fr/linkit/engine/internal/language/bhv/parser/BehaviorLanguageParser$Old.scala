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

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.parser.ASTTokens._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.reflect.{ClassTag, classTag}
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageParser$Old extends RegexParsers {
/*
    override val whiteSpace    = "\\s+".r
    private var skipWhiteSpace = true

    override def skipWhitespace = skipWhiteSpace

    ////////MISC
    private val modifiers = rep(
        scalaCodeIntegration("distant_to_current", RemoteToCurrentModifier)
                | scalaCodeIntegration("current_to_distant", CurrentToRemoteModifier)
                | scalaCodeIntegration("current_to_distant_event", CurrentToRemoteEvent)
                | scalaCodeIntegration("distant_to_current_event", RemoteToCurrentEvent)
    )

    /////////IMPORT PARSER
    private val importExp = "import " ~> ("[^\\s]+".r ^^ ImportToken)

    ////////CLASS PARSER
    private final val ReturnValue      = "returnvalue"
    private final val ForcedSync       = SynchronizeState(true, true)
    private final val NotForcedNotSync = SynchronizeState(true, false)
    private       val classDescribe    = {
        val syncOrNot = "synchronize" ^^^ ForcedSync | "!synchronize".r ^^^ SynchronizeState(true, false)
        val reference = ("as" ~> ("@\\w+".r ^^ (x => Some(ExternalReference(x))) | "\\w+".r ^^ (x => Some(InternalReference(x))))) ||| "" ^^^ None

        val classHead        = "describe" ~> ((("static class" ^^^ true | "class" ^^^ false) ~ "[^\\s]+".r ~ reference)
                ^^ { case isStatic ~ ref ~ referent => ClassDescriptionHead(isStatic, ref, referent) })
        val methodSignature  = "\\w+\\(".r ~ "([\\w,.\\s\\[\\]]+)\\)".r ^^ { case name ~ signature =>
            MethodSignature(name.dropRight(1), '(' + signature, null, parseSyncParams(signature))
        }
        val disableMethod    = ("disable" ||| "disable" ~ "method") ~> methodSignature ^^ (x => DisabledMethodDescription(Some(x)))
        val enableMethodHead = ("enable" ||| "enable" ~ "method") ~> methodSignature
        val enableMethodCore = {
            val param = "\\d+".r | ReturnValue

            def and: Parser[List[String]] = (param ~ "&" ~ and) ^^ { case hd ~ _ ~ tail => hd :: tail } | param ^^ (List(_))

            reference ~ ("{" ~>
                    rep(("modify" ~> and <~ "{") ~ modifiers <~ "}" ^^ {
                        case concernedComps ~ lambdas => MethodComponentsModifier(
                            concernedComps.filter(_ != ReturnValue).map(i => (i.toInt, lambdas)).toMap,
                            if (concernedComps.contains(ReturnValue)) lambdas else Seq())
                    })
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

    ////////TYPE MODIFIER PARSER
    private val typeModifier = ("modifier " ~> "[^\\s]+".r <~ "{") ~ modifiers ^^ {
        case clazz ~ modifiers => TypeModifiers(clazz, modifiers)
    }

    ////////SCALA INTEGRATION PARSER
    private val scalaCodePrefix = "${" f "'${' not specified before scala code expression"
    private val scalaCode       = "scala" ~> scalaCodePrefix ~> codeBlock ^^ toScalaCodeToken

    private def keep[X: ClassTag](s: Seq[BHVLangToken]): Seq[X] = {
        val cl = classTag[X].runtimeClass
        s.filter(o => cl.isAssignableFrom(o.getClass)).map(_.asInstanceOf[X])
    }

    private def codeBlock: Parser[String] = {
        var bracketDepth = 1

        def code: Parser[String] = {
            skipWhiteSpace = false
            ("[\\s\\S]+?[}{]".r ^^ { s =>
                if (s.last == '{') bracketDepth += 1 else bracketDepth -= 1
                skipWhiteSpace = true
                s
            }) ~ (if (bracketDepth == 0) "" else code) ^^ { case a ~ b => a + b }
        }

        code
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {

        name ~ "->" ~> scalaCodePrefix ~> codeBlock ^^ { exp =>
            LambdaExpression(toScalaCodeToken(exp), kind)
        }
    }

    private def reformatCode(s: String): String = {
        val firstLineIndex = s.indexOf('\n')
        (s.take(firstLineIndex).stripLeading() + s.drop(firstLineIndex).stripIndent()).dropRight(1)
    }

    private def toScalaCodeToken(code: String): ScalaCodeBlock = {
        val formattedCode = reformatCode(code)
        var pos           = 0


        val externalReference = "[\\s\\S]+?£\\{".r ~> ".[\\w/]+".r ~ (":" ~> "[^}]+".r).? <~ "}" ^^ {
            case name ~ clazz =>
                pos = formattedCode.indexOf("£{", pos + 1)
                ScalaCodeExternalObject(name, clazz.getOrElse("scala.Any"), pos)
        }
        val refs = parse(rep(externalReference), code) match {
            case Success(x, _)     =>
                x
            case NoSuccess(msg, n) =>
                val errorMsg = makeErrorMessage(msg, "Failure into scala block code", new CharSequenceReader(formattedCode), n.pos)
                throw new BHVLanguageException(errorMsg)
        }
        ScalaCodeBlock(formattedCode, refs)
    }

    private val synchronisePattern = "\\s*synchronize\\s*.*".r.pattern

    private def parseSyncParams(signature: String): Seq[Int] = {
        signature.split(",")
                .zipWithIndex
                .filter { case (str, _) => synchronisePattern.matcher(str).matches() }
                .map(_._2)
    }

    /////////FILE PARSER

    private val fileParser = rep(classDescribe | importExp | typeModifier | scalaCode)

    def parse(input: CharSequenceReader): List[RootToken] = {
        parseAll(fileParser, input) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(makeErrorMessage(msg, "Failure", input, n.pos))
            case Success(r, _)     => r
        }
    }

    implicit class ParserOps[P](that: Parser[P]) {

        def e(msg: String): Parser[P] = that withErrorMessage msg

        def f(msg: String): Parser[P] = that withFailureMessage msg
    }

    implicit class StringParserOps[P](that: String) extends ParserOps[String](that)*/
}
