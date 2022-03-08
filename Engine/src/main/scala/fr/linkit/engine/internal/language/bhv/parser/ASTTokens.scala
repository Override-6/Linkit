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

import fr.linkit.api.gnom.cache.sync.contract.RemoteObjectInfo

object ASTTokens {

    sealed trait BHVLangToken
    sealed trait RootToken extends BHVLangToken

    case class ImportToken(className: String) extends RootToken

    case class SynchronizeState(forced: Boolean, value: Boolean)

    case class ClassDescriptionHead(static: Boolean, className: String,
                                    referent: Option[ValueReference], remoteObjectInfo: Option[RemoteObjectInfo])
            extends BHVLangToken
    case class ClassDescription(head: ClassDescriptionHead,
                                foreachMethod: Option[MethodDescription],
                                foreachField: Option[FieldDescription],
                                methods: Seq[MethodDescription],
                                fields: Seq[FieldDescription]) extends RootToken

    case class MethodParam(synchronized: Boolean, tpe: String) extends BHVLangToken {
        override def toString: String = (if (synchronized) "synchronized " else "") + tpe
    }
    case class MethodSignature(methodName: String, params: Seq[MethodParam]) extends BHVLangToken {
        override def toString: String = s"$methodName${params.mkString("(", ",", ")")}"
    }
    case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression]) extends BHVLangToken
    case class FieldDescription(state: SynchronizeState, fieldName: Option[String]) extends BHVLangToken


    case class TypeModifiers(className: String, modifiers: Seq[LambdaExpression]) extends RootToken
    case class ScalaCodeExternalObject(name: String, typeName: String, pos: Int) extends BHVLangToken
    case class ScalaCodeBlock(sourceCode: String, objects: Seq[ScalaCodeExternalObject]) extends RootToken


    trait MethodDescription extends BHVLangToken
    case class DisabledMethodDescription(signature: Option[MethodSignature]) extends MethodDescription
    case class HiddenMethodDescription(signature: Option[MethodSignature], hideMessage: Option[String]) extends MethodDescription
    case class EnabledMethodDescription(referent: Option[ValueReference],
                                        procrastinatorName: Option[ExternalReference],
                                        signature: Option[MethodSignature],
                                        modifiers: Seq[MethodComponentsModifier],
                                        syncReturnValue: SynchronizeState) extends MethodDescription

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
