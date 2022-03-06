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

    case class ClassDescriptionHead(static: Boolean, className: String, referent: Option[ValueReference]) extends BHVLangToken
    case class ClassDescription(head: ClassDescriptionHead, methods: Seq[MethodDescription], fields: Seq[FieldDescription]) extends BHVLangToken

    case class MethodSignature(methodName: String, signature: String, params: Seq[String], synchronisedParams: Seq[Int]) extends BHVLangToken
    case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression]) extends BHVLangToken
    case class FieldDescription(state: SynchronizeState, fieldName: Option[String]) extends BHVLangToken

    case class TypeModifiers(className: String, modifiers: Seq[LambdaExpression]) extends BHVLangToken
    case class ScalaCodeExternalObject(name: String, clazz: String, pos: Int) extends BHVLangToken
    case class ScalaCodeBlock(code: String, objects: Seq[ScalaCodeExternalObject]) extends BHVLangToken


    trait MethodDescription extends BHVLangToken
    case class DisabledMethodDescription(signature: Option[MethodSignature]) extends MethodDescription
    case class HiddenMethodDescription(signature: Option[MethodSignature], errorMessage: String) extends MethodDescription
    case class EnabledMethodDescription(referent: Option[ValueReference], signature: Option[MethodSignature],
                                        syncReturnValue: SynchronizeState, modifiers: Seq[MethodComponentsModifier]) extends MethodDescription

    trait LambdaKind
    case object CurrentToRemoteModifier extends LambdaKind
    case object RemoteToCurrentModifier extends LambdaKind
    case object CurrentToRemoteEvent extends LambdaKind
    case object RemoteToCurrentEvent extends LambdaKind
    case class LambdaExpression(code: ScalaCodeBlock, kind: LambdaKind) extends BHVLangToken

    trait ValueReference extends BHVLangToken
    //Internal reference points a reference to a method/class/field that is declared into the file
    case class InternalReference(name: String) extends ValueReference
    //External reference points a reference that is external from the file, it's a value that is contained into a property class.
    case class ExternalReference(name: String) extends ValueReference

}
