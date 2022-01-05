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

package fr.linkit.engine.internal.language.bhv

import scala.util.parsing.combinator.RegexParsers

object Tokens extends RegexParsers {

    sealed trait WorkflowToken

    //Basic token declaration
    object Import extends WorkflowToken

    object Describe extends WorkflowToken

    object Class extends WorkflowToken

    object Method extends WorkflowToken

    object Field extends WorkflowToken

    object As extends WorkflowToken

    object Enable extends WorkflowToken

    object Hide extends WorkflowToken

    object Forall extends WorkflowToken

    object Returnvalue extends WorkflowToken

    object Modify extends WorkflowToken

    object Static extends WorkflowToken

    object BracketOpen extends WorkflowToken

    object BracketClose extends WorkflowToken

    object And extends WorkflowToken

    final val `import`    : Parser[WorkflowToken] = Import
    final val describe    : Parser[WorkflowToken] = Describe
    final val `class`     : Parser[WorkflowToken] = Class
    final val method      : Parser[WorkflowToken] = Describe
    final val field       : Parser[WorkflowToken] = Field
    final val as          : Parser[WorkflowToken] = As
    final val enable      : Parser[WorkflowToken] = Enable
    final val hide        : Parser[WorkflowToken] = Hide
    final val forall      : Parser[WorkflowToken] = Forall
    final val returnvalue : Parser[WorkflowToken] = Returnvalue
    final val modify      : Parser[WorkflowToken] = Modify
    final val static      : Parser[WorkflowToken] = Static
    final val bracketOpen : Parser[WorkflowToken] = "\\{".r ^^^ BracketOpen
    final val bracketClose: Parser[WorkflowToken] = "}" ^^^ BracketClose
    final val and         : Parser[WorkflowToken] = "&" ^^^ And

    implicit private def toParser(token: WorkflowToken): Parser[WorkflowToken] = {
        token.getClass.getSimpleName.toLowerCase ^^^ token
    }

    //other tokens
    case class MethodSignature(signature: String) extends WorkflowToken
    case class Literal(str: String) extends WorkflowToken
    case class Number(str: String) extends WorkflowToken

    final val signature: Parser[WorkflowToken] = "([^(].+)(\\([\\w, \\[\\]].*?\\))\\s".r ^^ MethodSignature
    final val literal  : Parser[WorkflowToken] = "\\\"(\\\\.|[^\"\\\\])*\\\"".r ^^ Literal
    final val number: Parser[WorkflowToken] = "[0-9].*" ^^ Number

}
