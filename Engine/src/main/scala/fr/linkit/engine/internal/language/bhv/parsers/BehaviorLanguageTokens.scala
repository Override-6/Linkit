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

    case class ClassReference(className: String) extends BHVLangToken
    case class ClassDescriptionHead(static: Boolean, ref: ClassReference) extends BHVLangToken
    case class ClassDescription(head: ClassDescriptionHead, methods: Seq[EnabledMethodDescription], fields: Seq[FieldDescription]) extends BHVLangToken

    case class MethodSignature(signature: String, synchronisedParams: Seq[Int]) extends BHVLangToken
    case class DisabledMethodDescription(signature: MethodSignature) extends BHVLangToken
    case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression]) extends BHVLangToken
    case class EnabledMethodDescription(signature: MethodSignature, syncReturnValue: Boolean, modifiers: Seq[MethodComponentsModifier]) extends BHVLangToken
    case class FieldDescription(synchronize: Boolean, fieldName: String) extends BHVLangToken

    trait LambdaKind
    object CurrentToRemoteModifier extends LambdaKind
    object RemoteToCurrentModifier extends LambdaKind
    object CurrentToRemoteEvent extends LambdaKind
    object RemoteToCurrentEvent extends LambdaKind
    case class LambdaExpression(expression: String, kind: LambdaKind) extends BHVLangToken
}
