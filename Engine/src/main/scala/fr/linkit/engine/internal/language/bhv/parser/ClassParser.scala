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
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageToken

object ClassParser extends BehaviorLanguageParser {

    override type Elem = BehaviorLanguageToken
    private val classParser = {
        val syncOrNot                  = (Exclamation.? <~ Sync ^^ (_.isEmpty)).? ^^ (s => SynchronizeState(s.isDefined, s.getOrElse(false)))
        val properties                 = {
            val property = SquareBracketLeft ~> (identifier <~ Equal) ~ identifier <~ SquareBracketRight ^^ { case name ~ value => MethodProperty(name, value) }
            repsep(property, Comma.?)
        }
        val methodModifierParser       = {
            ((identifier | ReturnValue ^^^ "returnvalue") <~ Arrow) ~ (identifier | modifiers) ^^ {
                case target ~ (ref: String)                 => ValueModifierReference(target, ref)
                case target ~ (mods: Seq[LambdaExpression]) => ModifierExpression(target, mods.find(_.kind == In), mods.find(_.kind == Out))
            }
        }
        val returnvalueState           = syncOrNot <~ ReturnValue | success(SynchronizeState(false, false))
        val as                         = As ~> identifier ^^ AgreementReference
        val foreachMethodEnable        = {
            val notModifiers = not(rep1(methodModifierParser)) withFailureMessage "Global method description can't have any modifier"
            (properties <~ Foreach <~ Method <~ Enable) ~ as.? ~ (BracketLeft ~> notModifiers ~> returnvalueState <~ notModifiers <~ BracketRight).? ^^ {
                case properties ~ ref ~ rvState => new EnabledMethodDescription(properties, ref, rvState.getOrElse(SynchronizeState(false, false)))
            }
        }
        val foreachMethodDisable       = Foreach ~ Method ~> Disable ^^^ new DisabledMethodDescription()
        val foreachMethod              = foreachMethodEnable | foreachMethodDisable
        val foreachFields              = syncOrNot <~ Star ^^ (new FieldDescription(_))
        val methodSignature            = {
            val param  = syncOrNot ~ (identifier <~ Colon).? ~ typeParser ^^ { case sync ~ name ~ id => MethodParam(sync, name, id) }
            val params = repsep(param, Comma)

            identifier ~ (ParenLeft ~> params <~ ParenRight).? ^^ { case name ~ params => MethodSignature(name, params.getOrElse(Seq())) }
        }
        val enabledMethodCore          = {
            (BracketLeft ~> rep(methodModifierParser) ~ returnvalueState <~ BracketRight) | success(List() ~~ SynchronizeState(false, false))
        }
        val enabledMethodParser        = {
            properties ~ (Enable.? ~> methodSignature) ~ as.? ~ enabledMethodCore ^^ {
                case properties ~ sig ~ referent ~ (modifiers ~ syncRv) => EnabledMethodDescription(properties, referent, syncRv)(sig, modifiers)
            }
        }
        val disabledMethodParser       = {
            Disable ~> methodSignature ^^ (DisabledMethodDescription(_))
        }
        val hiddenMethodParser         = {
            Hide ~> Method ~> methodSignature ~ literal.? ^^ { case sig ~ msg => HiddenMethodDescription(msg)(sig) }
        }
        val methodsParser              = enabledMethodParser | disabledMethodParser | hiddenMethodParser
        val fieldsParser               = syncOrNot ~ identifier ^^ { case state ~ name => AttributedFieldDescription(name, state) }
        val classHead                  = Describe ~> (Statics | Mirroring).? ~ identifier ~ (Stub ~> identifier).? ^^ {
            case Some(Mirroring) ~ className ~ stubClass => ClassDescriptionHead(MirroringDescription(stubClass.getOrElse(className)), className)
            case None ~ className ~ None                 => ClassDescriptionHead(RegularDescription, className)
            case Some(Statics) ~ className ~ None        => ClassDescriptionHead(StaticsDescription, className)
            case _@(Some(Statics) | None) ~ className ~ Some(_)  =>
                throw new BHVLanguageException(s"statics or regular description '${className}' cannot define a stub class.")
        }
        val attributedFieldsAndMethods = rep(methodsParser | fieldsParser) ^^ { x =>
            val fields  = x.filter(_.isInstanceOf[AttributedFieldDescription]).map { case d: AttributedFieldDescription => d }
            val methods = x.filter(_.isInstanceOf[AttributedMethodDescription]).map { case d: AttributedMethodDescription => d }
            fields ~~ methods
        }
        (classHead <~ BracketLeft) ~ foreachFields.? ~ foreachMethod.? ~ attributedFieldsAndMethods <~ BracketRight ^^ {
            case head ~ foreachFields ~ foreachMethods ~ (fields ~ methods) => ClassDescription(head, foreachMethods, foreachFields, fields, methods)
        }
    }

    private[parser] def parser: Parser[ClassDescription] = classParser

}
