/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory
import fr.linkit.api.connection.cache.obj.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.{ReturnValueBehavior, ReturnValueModifier}
import fr.linkit.api.connection.cache.obj.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.connection.cache.obj.behavior.{ObjectBehavior, ObjectBehaviorStore}
import fr.linkit.api.connection.cache.obj.description.SyncObjectSuperclassDescription
import org.jetbrains.annotations.Nullable

class DefaultObjectBehavior[A <: AnyRef] protected(override val classDesc: SyncObjectSuperclassDescription[A],
                                                   factory: MemberBehaviorFactory,
                                                   whenFieldModifier: Option[FieldModifier[A]],
                                                   whenParameterModifier: Option[ParameterModifier[A]],
                                                   whenReturnValueModifier: Option[ReturnValueModifier[A]]) extends ObjectBehavior[A] {

    private val methods = {
        generateMethodsBehavior()
                .map(bhv => bhv.desc.methodId -> bhv)
                .toMap
    }

    private val fields = {
        generateFieldsBehavior()
                .map(bhv => bhv.desc.fieldId -> bhv)
                .toMap
    }

    override def listMethods(): Iterable[MethodBehavior] = {
        methods.values
    }

    override def getMethodBehavior(id: Int): Option[InternalMethodBehavior] = methods.get(id)

    override def listField(): Iterable[FieldBehavior[AnyRef]] = {
        fields.values
    }

    override def getFieldBehavior(id: Int): Option[FieldBehavior[AnyRef]] = fields.get(id)

    protected def generateMethodsBehavior(): Iterable[InternalMethodBehavior] = {
        classDesc.listMethods()
                .map(factory.genMethodBehavior(None, _))
    }

    protected def generateFieldsBehavior(): Iterable[FieldBehavior[AnyRef]] = {
        classDesc.listFields()
                .map(factory.genFieldBehavior)
    }

    override def whenField: Option[FieldModifier[A]] = whenFieldModifier

    override def whenParameter: Option[ParameterModifier[A]] = whenParameterModifier

    override def whenMethodReturnValue: Option[ReturnValueModifier[A]] = whenReturnValueModifier
}

object DefaultObjectBehavior {

    def apply[A <: AnyRef](classDesc: SyncObjectSuperclassDescription[A], tree: ObjectBehaviorStore,
                           @Nullable whenFieldModifier: FieldModifier[A],
                           @Nullable whenParameterModifier: ParameterModifier[A],
                           @Nullable whenReturnValueModifier: ReturnValueModifier[A]): DefaultObjectBehavior[A] = {
        new DefaultObjectBehavior(classDesc, tree.factory, Option(whenFieldModifier), Option(whenParameterModifier), Option(whenReturnValueModifier))
    }

}
