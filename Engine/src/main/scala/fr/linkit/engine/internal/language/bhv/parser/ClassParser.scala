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
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._

object ClassParser extends BehaviorLanguageParser {

    private val classParser = {
        val syncParser = {
            val options = Sync ^^^ Synchronized | Chip ^^^ Chipped | BehaviorLanguageKeyword.Mirror ^^^ SyncLevel.Mirror | Regular ^^^ NotRegistered
            options.? ^^ (opt => RegistrationState(opt.isDefined, opt.getOrElse(NotRegistered)))
        }
        val properties = {
            val property = SquareBracketLeft ~> (identifier <~ Equal) ~ (At ~> identifier) <~ SquareBracketRight ^^ { case name ~ value => MethodProperty(name, value) }
            repsep(property, Comma.?)
        }

        val dispatcher              = {
            // we can either reference an agreement or define one directly into this method declaration.
            val anb = AgreementBuilderParser.anonymous
            val ref = identifier ^^ AgreementReference
            FatArrow ~> ((((ref.? <~ Colon) ~ anb) ^^ {
                case ref ~ agreement => AgreementBuilder(ref.map(_.name), agreement.instructions)
            }) | anb | ref)
        }
        val invHandlingTypeParser   = (Ensinv ^^^ EnableSubInvocations | Disinv ^^^ DisableSubInvocations) | success(Inherit)
        val foreachMethodParameters = ParenLeft ~ ParenRight withErrorMessage "generic method definitions cannot contain parameter"
        val foreachMethodEnable     = {
            // val notModifiers = not(rep1(methodModifierParser)) withFailureMessage "Global method description can't have any modifier"
            (properties ~ invHandlingTypeParser <~ Enable <~ Star <~ foreachMethodParameters) ~ (Colon ~> syncParser).? ~ dispatcher.? ^^ {
                case properties ~ invMethod ~ rvState ~ agreement =>
                    val state = rvState.getOrElse(RegistrationState(false, NotRegistered))
                    new EnabledMethodDescription(invMethod, properties, agreement, state)
            }
        }
        val foreachMethodDisable    = Disable ~ Star ~ foreachMethodParameters ^^^ new DisabledMethodDescription()
        val foreachMethod           = foreachMethodEnable | foreachMethodDisable
        val foreachFields           = syncParser <~ Star ^^ (new FieldDescription(_))
        val methodSignature         = {
            val synchronizedType = syncParser ~ typeParser.? ^^ { case syncLevel ~ tpe => SynchronizedType(syncLevel, tpe) }
            val param            = syncParser ~ (identifier <~ Colon).? ~ typeParser ^^ { case syncLevel ~ name ~ tpe => MethodParam(name, SynchronizedType(syncLevel, Some(tpe))) }
            val params           = repsep(param, Comma)

            (identifier <~ Dot).? ~ identifier ~ (ParenLeft ~> params <~ ParenRight).? ~ (Colon ~> synchronizedType).? ^^ {
                case targetClass ~ name ~ params ~ rstpe =>
                    MethodSignature(targetClass, name, params.getOrElse(Seq()), rstpe)
            }
        }

        val enabledMethodParser        = {
            invHandlingTypeParser ~ properties ~ (Enable.? ~> methodSignature) ~ dispatcher.? ^^ {
                case invMethod ~ properties ~ sig ~ agreement =>
                    EnabledMethodDescription(invMethod, properties, agreement)(sig)
            }
        }
        val disabledMethodParser       = {
            Disable ~> methodSignature ^^ (DisabledMethodDescription(_))
        }
        val hiddenMethodParser         = {
            Hide ~> Method.? ~> methodSignature ~ literal.? ^^ { case sig ~ msg => HiddenMethodDescription(msg)(sig) }
        }
        val methodsParser              = enabledMethodParser | disabledMethodParser | hiddenMethodParser
        val fieldsParser               = (identifier <~ Dot).? ~ syncParser ~ identifier ^^ { case clazz ~ state ~ name => AttributedFieldDescription(clazz, name, state) }
        val targetLevels               = {
            val stubParser = Stub ~> Colon ~> identifier | (ParenLeft ~> identifier <~ ParenRight) //two syntaxes are valid: "mirror stub: <stubclass>" or "mirror(<stubclass>)"
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
