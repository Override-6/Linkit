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
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

object BehaviorLanguageParser extends Parsers {

    override type Elem = BehaviorLanguageToken

    private val modifiers = BracketLeft ~> rep(
        scalaCodeIntegration("in", In) | scalaCodeIntegration("out", Out)
    ) <~ BracketRight

    private val identifier        = accept("identifier", { case Identifier(identifier) => identifier })
    private val literal           = accept("literal", { case Literal(str) => str })
    private val codeBlock         = accept("scala code", { case CodeBlock(code) => toScalaCodeToken(code) })
    private val externalReference = identifier ^^ ExternalReference
    private val any               = Parser[Unit] { in => Success((), in.rest) }
    private val typeParser        = identifier ~ (SquareBracketLeft ~ SquareBracketRight).? ^^ {
        case str ~ postfix => if (postfix.isDefined) str + "[]" else str
    }

    private val importParser        = Import ~> identifier ^^ ClassImport
    private val classParser         = {
        val returnvalue          = ReturnValue ^^^ "returnvalue"
        val syncOrNot            = (Not.? <~ Sync ^^ (_.isEmpty)).? ^^ (s => SynchronizeState(s.isDefined, s.getOrElse(false)))
        val methodSignature      = {
            val param  = syncOrNot ~ (identifier <~ Colon).? ~ typeParser ^^ { case sync ~ name ~ id => MethodParam(sync, name, id) }
            val params = repsep(param, Comma)

            (identifier <~ ParenLeft) ~ params <~ ParenRight ^^ { case name ~ params => MethodSignature(name, params) }
        }
        val methodModifierParser = {
            ((identifier | returnvalue) <~ Arrow) ~ (identifier | modifiers) ^^ {
                case target ~ (ref: String)                 => ValueModifierReference(target, ref)
                case target ~ (mods: Seq[LambdaExpression]) => ModifierExpression(target, mods.find(_.kind == In), mods.find(_.kind == Out))
            }
        }
        val enabledMethodCore    = {
            (BracketLeft ~> rep(methodModifierParser) ~ (syncOrNot <~ ReturnValue.?) <~ BracketRight) | success(List() ~~ SynchronizeState(false, false))
        }
        val properties           = {
            val property = SquareBracketLeft ~> (identifier <~ Equal) ~ identifier <~ SquareBracketRight ^^ { case name ~ value => MethodProperty(name, value) }
            repsep(property, Comma | success())
        }
        val enabledMethodParser  = {
            properties ~ (Enable ~> Method ~> methodSignature) ~ (As ~> externalReference).? ~ enabledMethodCore ^^ {
                case properties ~ sig ~ referent ~ (modifiers ~ syncRv) => EnabledMethodDescription(properties, referent, None, modifiers, syncRv)(sig)
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
    private val codeBlockParser     = Scala ~> codeBlock
    private val typeModifierParser  = Modifier ~> identifier ~ modifiers ^^ { case tpe ~ modifiers => TypeModifier(tpe, modifiers) }
    private val valueModifierParser = {
        (identifier <~ Colon) ~ (typeParser <~ Arrow) ~ modifiers ^^ { case name ~ tpe ~ modifiers => ValueModifier(name, tpe, modifiers) }
    }

    private def scalaCodeIntegration(name: String, kind: LambdaKind): Parser[LambdaExpression] = {
        Identifier(name) ~ Colon ~> codeBlock ^^ (LambdaExpression(_, kind))
    }

    private def toScalaCodeToken(sc: String): ScalaCodeBlock = {
        ScalaCodeBlocksParser.parse(new CharSequenceReader(sc))
    }

    implicit class Tilde[A](a: A) {

        def ~~[B](b: B): A ~ B = new ~(a, b)
    }

    private val fileParser = phrase(rep(importParser | classParser | codeBlockParser | typeModifierParser | valueModifierParser))

    def parse(context: ParserContext[Elem]): BehaviorFile = {
        fileParser.apply(new TokenReader(context.fileTokens)) match {
            case NoSuccess(msg, n) =>
                throw new BHVLanguageException(makeErrorMessage(msg, "Failure", n.pos, context.fileSource, context.filePath))
            case Success(x, _)     =>
                unpack(x)
        }
    }

    private def unpack(roots: List[Product]): BehaviorFile = {
        val (imports, classes, blocks, tpeMods, valMods) = roots.foldLeft(
            (List[ClassImport](), List[ClassDescription](), List[ScalaCodeBlock](), List[TypeModifier](), List[ValueModifier]())
        ) {
            case ((imports, b, c, d, e), imp: ClassImport)        => (imp :: imports, b, c, d, e)
            case ((a, classes, c, d, e), clazz: ClassDescription) => (a, clazz :: classes, c, d, e)
            case ((a, b, blocks, d, e), block: ScalaCodeBlock)    => (a, b, block :: blocks, d, e)
            case ((a, b, c, tpeMods, e), mod: TypeModifier)       => (a, b, c, mod :: tpeMods, e)
            case ((a, b, c, d, valMods), mod: ValueModifier)      => (a, b, c, d, mod :: valMods)
        }
        new BehaviorFile {
            override val classDescriptions = classes
            override val typesModifiers    = tpeMods
            override val codeBlocks        = blocks
            override val classImports      = imports
            override val valueModifiers    = valMods
        }
    }

}