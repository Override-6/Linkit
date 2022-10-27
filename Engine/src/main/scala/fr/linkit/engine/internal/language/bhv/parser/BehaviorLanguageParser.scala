/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageValues._
import fr.linkit.engine.internal.language.bhv.lexer.file.{BehaviorLanguageKeyword, BehaviorLanguageToken}

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

abstract class BehaviorLanguageParser extends Parsers {
    
    override type Elem = BehaviorLanguageToken
    
    //HERE ARE COMMON PARSERS
    protected val modifiers  = {
        val mod = scalaCodeIntegration(BehaviorLanguageKeyword.In, In) | scalaCodeIntegration(BehaviorLanguageKeyword.Out, Out)
        (BracketLeft ~> rep(mod) <~ BracketRight) | mod ^^ (List(_))
    }
    protected val identifier = accept("identifier", { case Identifier(identifier) => identifier })
    protected val typeParser = identifier ~ (SquareBracketLeft ~ SquareBracketRight).? ^^ {
        case str ~ postfix => if (postfix.isDefined) str + "[]" else str
    }
    protected val literal    = accept("literal", { case Literal(str) => str })
    protected val number     = accept("number", { case Number(n) => n.toDouble })
    protected val bool       = accept("bool", { case Bool(b) => java.lang.Boolean.valueOf(b).booleanValue() })
    protected val codeBlock  = accept("scala code", { case CodeBlock(code) => toScalaCodeToken(code) })
    
    private def scalaCodeIntegration(token: BehaviorLanguageToken, kind: LambdaKind): Parser[LambdaExpression] = {
        token ~ Colon ~> codeBlock ^^ (LambdaExpression(_, kind))
    }
    
    private def toScalaCodeToken(sc: String): ScalaCodeBlock = {
        ScalaCodeBlocksParser.parse(new CharSequenceReader(sc))
    }
    
    implicit class Tilde[A](a: A) {
        
        def ~~[B](b: B): A ~ B = new ~(a, b)
    }
    
    protected implicit def acceptForeign[P](parser: BehaviorLanguageParser#Parser[P]): Parser[P] = Parser { in =>
        parser(in) match {
            case e: BehaviorLanguageParser#Success[P] => Success(e.result, e.next)
            case e: BehaviorLanguageParser#Failure    => Failure(e.msg, e.next)
            case e: BehaviorLanguageParser#Error      => Error(e.msg, e.next)
        }
    }
    
    protected def log[P](parser: Parser[P]): Parser[P] = super.log(parser)(parser.toString())
    
}
