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

object BehaviorLanguageTokens {

    trait BHVLangToken

    case class ImportToken(className: String) extends BHVLangToken

    case class SynchronizeState(forced: Boolean, value: Boolean)

    case class ClassReference(className: String) extends BHVLangToken
    case class ClassDescriptionHead(static: Boolean, ref: ClassReference) extends BHVLangToken
    case class ClassDescription(head: ClassDescriptionHead, methods: Seq[MethodDescription], fields: Seq[FieldDescription]) extends BHVLangToken

    case class MethodReference(referencedMethod: String) extends BHVLangToken
    case class MethodSignature(methodName: String, signature: String, synchronisedParams: Seq[Int]) extends BHVLangToken
    case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression]) extends BHVLangToken
    case class FieldDescription(state: SynchronizeState, fieldName: String) extends BHVLangToken

    trait MethodDescription extends BHVLangToken
    case class DisabledMethodDescription(signature: MethodSignature) extends MethodDescription
    case class HiddenMethodDescription(signature: MethodSignature, errorMessage: String) extends MethodDescription
    case class EnabledMethodDescription(referentMethod: Option[MethodReference], signature: MethodSignature,
                                        syncReturnValue: SynchronizeState, modifiers: Seq[MethodComponentsModifier]) extends MethodDescription

    trait LambdaKind
    case object CurrentToRemoteModifier extends LambdaKind
    case object RemoteToCurrentModifier extends LambdaKind
    case object CurrentToRemoteEvent extends LambdaKind
    case object RemoteToCurrentEvent extends LambdaKind
    case class LambdaExpression(expression: String, kind: LambdaKind) extends BHVLangToken
}
