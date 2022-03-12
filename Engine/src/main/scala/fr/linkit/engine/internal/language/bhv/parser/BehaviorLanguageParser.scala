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
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageToken
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageValues._

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{CharSequenceReader, Position, Reader}

object BehaviorLanguageParser extends Parsers {

    override type Elem = BehaviorLanguageToken

    private val modifiers = BracketLeft ~> rep(
        scalaCodeIntegration("distant_to_current", RemoteToCurrentModifier)
            | scalaCodeIntegration("current_to_distant", CurrentToRemoteModifier)
            | scalaCodeIntegration("current_to_distant_event", CurrentToRemoteEvent)
            | scalaCodeIntegration("distant_to_current_event", RemoteToCurrentEvent)
    ) <~ BracketRight

    private val identifier        = accept("identifier", { case Identifier(identifier) => identifier })
    private val literal           = accept("literal", { case Literal(str) => str })
    private val codeBlock         = accept("scala code", { case CodeBlock(code) => toScalaCodeToken(code) })
    private val externalReference = identifier ^^ ExternalReference

    private val importParser       = Import ~> identifier ^^ ClassImport
    private val classParser        = {
        val syncOrNot            = (Not.? <~ Synchronize ^^ (_.isEmpty)).? ^^ (s => SynchronizeState(s.isDefined, s.getOrElse(false)))
        val methodSignature      = log{
            val param  = syncOrNot ~ identifier ^^ { case s ~ id => MethodParam(s, id) }
            val params = repsep(param, Comma)

            (identifier <~ ParenLeft) ~ params <~ ParenRight ^^ { case name ~ params => MethodSignature(name, params) }
        }("method signature")
        val methodModifierParser = {
            val param  = ReturnValue | accept("identifier", { case x: Identifier => x })
            val params = repsep(param, And)

            Modifier ~> params ~ modifiers ^^ {
                case concernedComps ~ modifiers => MethodComponentsModifier(
                    concernedComps.filterNot(_ == ReturnValue).map { case Identifier(num) => (num.toInt, modifiers) }.toMap,
                    if (concernedComps.contains(ReturnValue)) modifiers else Seq())
            }
        }
        val enabledMethodCore    = {
            (BracketLeft ~> rep(methodModifierParser) ~ (syncOrNot <~ ReturnValue.?) <~ BracketRight) | success(List() ~~ SynchronizeState(false, false))
        }
        val enabledMethodParser  = {
            (Enable ~> Method ~> methodSignature) ~ (As ~> externalReference).? ~ enabledMethodCore ^^ {
                case sig ~ referent ~ (modifiers ~ syncRv) => EnabledMethodDescription(referent, None, modifiers, syncRv)(sig)
            }
        }
        val disabledMethodParser = {
            Disable ~> Method ~> methodSignature ^^ (DisabledMethodDescription(_))
        }
        val hiddenMethodParser   = {
            Hide ~> Method ~> methodSignature ~ literal.? ^^ { case sig ~ msg => HiddenMethodDescription(msg)(sig) }
        }
        val methodsParser        = enabledMethodParser | disabledMethodParser | hiddenMethodParser

        val fieldsParser = syncOrNot ~ identifier ^^ { case state ~ name => AttributedFieldDescription(name, state) }

        val classHead                  = Describe ~> (Statics | Mirroring).? ~ identifier ~ (Stub ~> identifier).? ^^ {
            case Some(Mirroring) ~ className ~ stubClass => ClassDescriptionHead(MirroringDescription(stubClass.getOrElse(className)), className)
            case None ~ className ~ None                 => ClassDescriptionHead(RegularDescription, className)
            case Some(Statics) ~ className ~ None        => ClassDescriptionHead(StaticsDescription, className)
            case _@(Some(Statics) | None) ~ _ ~ Some(_)  => throw new BHVLanguageException("statics or regular description cannot define a stub class.")
        }
        val attributedFieldsAndMethods = rep(methodsParser | fieldsParser) ^^ { x =>
            val fields  = x.filter(_.isInstanceOf[AttributedFieldDescription]).map { case d: AttributedFieldDescription => d }
            val methods = x.filter(_.isInstanceOf[AttributedMethodDescription]).map { case d: AttributedMethodDescription => d }
            fields ~~ methods
        }
        (classHead <~ BracketLeft) ~ attributedFieldsAndMethods <~ BracketRight ^^ {
            case head ~ (fields ~ methods) => ClassDescription(head, None, None, fields, methods)
        }
    }
    private val codeBlockParser    = Scala ~> codeBlock
    private val typeModifierParser = Modifier ~> identifier ~ modifiers

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        Identifier(name) ~ Arrow ~> codeBlock ^^ (LambdaExpression(_, kind))
    }

    private def toScalaCodeToken(sc: String): ScalaCodeBlock = {
        ScalaCodeBlocksParser.parse(new CharSequenceReader(sc))
    }

    implicit class Tilde[A](a: A) {

        def ~~[B](b: B): A ~ B = new ~(a, b)
    }

    private val fileParser = phrase(rep(importParser | classParser | codeBlockParser | typeModifierParser))

    def parse(tokens: Seq[Elem]): BehaviorFile = {
        fileParser.apply(new TokenReader(tokens, 0)) match {
            case NoSuccess(msg, n) => throw new BHVLanguageException(s"Failure while parsing: $msg. at: ${n.pos}")
            case Success(x, _)     =>
                val (imports, classes, blocks, modifiers) = x.foldLeft(
                    (List[ClassImport](), List[ClassDescription](), List[ScalaCodeBlock](), List[TypeModifiers]())
                ) {
                    case ((imports, x, y, z), imp: ClassImport)        => (imp :: imports, x, y, z)
                    case ((w, classes, y, z), clazz: ClassDescription) => (w, clazz :: classes, y, z)
                    case ((w, x, blocks, z), block: ScalaCodeBlock)    => (w, x, block :: blocks, z)
                    case ((w, x, y, modifiers), imp: TypeModifiers)    => (w, x, y, imp :: modifiers)
                }
                new BehaviorFile {
                    override val classDescriptions = classes
                    override val typesModifiers    = modifiers
                    override val codeBlocks        = blocks
                    override val classImports      = imports
                }
        }
    }

    class TokenReader(tokens: Seq[Elem], offset: Int) extends Reader[Elem] {

        override def first: Elem = tokens.head

        override def rest: Reader[Elem] = new TokenReader(tokens.tail, offset + 1)

        override def pos: Position = new Position {
            override def line: Int = 0

            override def column: Int = offset

            override protected def lineContents: String = ""
        }

        override def atEnd: Boolean = tokens.isEmpty
    }

}