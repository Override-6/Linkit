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

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.engine.internal.language.bhv.ast
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._

object ClassParser extends BehaviorLanguageParser {
    
    private val classParser = {
        val syncParser = {
            val options = (Sync ^^^ Synchronized | Chip ^^^ Chipped | BehaviorLanguageKeyword.Mirror ^^^ SyncLevel.Mirror | Regular ^^^ NotRegistered).?
            (((Exclamation ^^^ true) ~ options) | (success(false) ~ options)) ^^ {
                case false ~ s => RegistrationState(s.isDefined, s.getOrElse(NotRegistered))
                case _         => RegistrationState(true, NotRegistered)
            }
        }
        val properties = {
            val property = SquareBracketLeft ~> (identifier <~ Equal) ~ (At ~> identifier) <~ SquareBracketRight ^^ { case name ~ value => MethodProperty(name, value) }
            repsep(property, Comma.?)
        }
        
        val methodModifierParser       = {
            ((identifier | ReturnValue ^^^ "returnvalue") <~ Arrow) ~ (identifier | modifiers) ^^ {
                case target ~ (ref: String)                 => ValueModifierReference(target, ref)
                case target ~ (mods: Seq[LambdaExpression]) => ModifierExpression(target, mods.find(_.kind == ast.In), mods.find(_.kind == ast.Out))
            }
        }
        val returnvalueState           = syncParser <~ ReturnValue | success(RegistrationState(false, NotRegistered))
        val following                  = Following ~> identifier ^^ AgreementReference
        val invHandlingTypeParser      = (Ensinv ^^^ EnableSubInvocations | Disinv ^^^ DisableSubInvocations) | success(Inherit)
        val foreachMethodEnable        = {
            val notModifiers = not(rep1(methodModifierParser)) withFailureMessage "Global method description can't have any modifier"
            (properties <~ Foreach <~ Method <~ Enable) ~ invHandlingTypeParser ~ following.? ~ (BracketLeft ~> notModifiers ~> returnvalueState <~ notModifiers <~ BracketRight).? ^^ {
                case properties ~ invMethod ~ agreementRef ~ rvState =>
                    val state = rvState.getOrElse(RegistrationState(false, NotRegistered))
                    new EnabledMethodDescription(invMethod, properties, agreementRef, state)
            }
        }
        val foreachMethodDisable       = Foreach ~ Method ~> Disable ^^^ new DisabledMethodDescription()
        val foreachMethod              = foreachMethodEnable | foreachMethodDisable
        val foreachFields              = syncParser <~ Star ^^ (new FieldDescription(_))
        val methodSignature            = {
            val param  = syncParser ~ (identifier <~ Colon).? ~ typeParser ^^ { case sync ~ name ~ id => MethodParam(sync, name, id) }
            val params = repsep(param, Comma)
            
            (identifier <~ Dot).? ~ identifier ~ (ParenLeft ~> params <~ ParenRight).? ^^ { case targetClass ~ name ~ params => MethodSignature(targetClass, name, params.getOrElse(Seq())) }
        }
        val enabledMethodCore          = {
            (BracketLeft ~> rep(methodModifierParser) ~ returnvalueState <~ BracketRight) | success(List() ~~ RegistrationState(false, NotRegistered))
        }
        val enabledMethodParser        = {
            invHandlingTypeParser ~ properties ~ (Enable.? ~> methodSignature) ~ following.? ~ enabledMethodCore ^^ {
                case invMethod ~ properties ~ sig ~ referent ~ (modifiers ~ syncRv) =>
                    EnabledMethodDescription(invMethod, properties, referent, syncRv)(sig, modifiers)
            }
        }
        val disabledMethodParser       = {
            Disable ~> methodSignature ^^ (DisabledMethodDescription(_))
        }
        val hiddenMethodParser         = {
            Hide ~> Method ~> methodSignature ~ literal.? ^^ { case sig ~ msg => HiddenMethodDescription(msg)(sig) }
        }
        val methodsParser              = enabledMethodParser | disabledMethodParser | hiddenMethodParser
        val fieldsParser               = (identifier <~ Dot).? ~ syncParser ~ identifier ^^ { case clazz ~ state ~ name => AttributedFieldDescription(clazz, name, state) }
        val targetLevels               = {
            val stubParser = ParenLeft ~> identifier <~ ParenRight
            val mirroring  = BehaviorLanguageKeyword.Mirror ~> stubParser.? ^^ MirroringLevel
            mirroring | (Sync ^^^ SynchronizeLevel) | (Chip ^^^ ChipLevel)
        }
        val targetedLevels             = SquareBracketLeft ~> rep1sep(targetLevels, Comma) <~ SquareBracketRight
        val targetedClasses            = rep1sep(identifier, Comma)
        val staticsHead                = Describe ~> BehaviorLanguageKeyword.Statics ~> targetedClasses ^^ (ClassDescriptionHead(StaticsDescription, _))
        val instanceHead               = Describe ~> targetedClasses ~ targetedLevels ^^ { case names ~ levels => ClassDescriptionHead(LeveledDescription(levels), names) }
        val regularHead                = Describe ~> targetedClasses ^^ (ClassDescriptionHead(RegularDescription, _))
        val classHead                  = staticsHead | instanceHead | regularHead
        val attributedFieldsAndMethods = rep(methodsParser | fieldsParser) ^^ { x =>
            val fields  = x.filter(_.isInstanceOf[AttributedFieldDescription]).map { case d: AttributedFieldDescription => d }
            val methods = x.filter(_.isInstanceOf[AttributedMethodDescription]).map { case d: AttributedMethodDescription => d }
            fields ~~ methods
        }
        classHead ~ (BracketLeft ~> foreachFields.? ~ foreachMethod.? ~ attributedFieldsAndMethods <~ BracketRight).? ^^ {
            case head ~ Some(foreachFields ~ foreachMethods ~ (fields ~ methods)) =>
                ClassDescription(head, foreachMethods, foreachFields, fields, methods)
            case head ~ None                                                      => ClassDescription(head, None, None, Nil, Nil)
        }
    }
    
    private[parser] val parser: Parser[ClassDescription] = classParser
    
}