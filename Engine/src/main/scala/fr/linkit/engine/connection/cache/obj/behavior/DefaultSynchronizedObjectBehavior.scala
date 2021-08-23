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
import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.connection.cache.obj.behavior.{SynchronizedObjectBehavior, SynchronizedObjectBehaviorStore}
import fr.linkit.api.connection.cache.obj.description.SyncObjectSuperclassDescription

class DefaultSynchronizedObjectBehavior[A <: AnyRef] protected(override val classDesc: SyncObjectSuperclassDescription[A],
                                                               factory: MemberBehaviorFactory,
                                                               asFieldBehavior: Option[FieldBehavior[A]],
                                                               asParameterBehavior: Option[ParameterBehavior[A]],
                                                               asReturnValueBehavior: Option[ReturnValueBehavior[A]]) extends SynchronizedObjectBehavior[A] {

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

    override def listField(): Iterable[FieldBehavior[Any]] = {
        fields.values
    }

    override def getFieldBehavior(id: Int): Option[FieldBehavior[Any]] = fields.get(id)

    protected def generateMethodsBehavior(): Iterable[InternalMethodBehavior] = {
        classDesc.listMethods()
            .map(factory.genMethodBehavior(None, _))
    }

    protected def generateFieldsBehavior(): Iterable[FieldBehavior[Any]] = {
        classDesc.listFields()
            .map(factory.genFieldBehavior)
    }

    override def whenField: Option[FieldBehavior[A]] = asFieldBehavior

    override def whenParameter: Option[ParameterBehavior[A]] = asParameterBehavior

    override def whenMethodReturnValue: Option[ReturnValueBehavior[A]] = asReturnValueBehavior
}

object DefaultSynchronizedObjectBehavior {

    def apply[A <: AnyRef](classDesc: SyncObjectSuperclassDescription[A], tree: SynchronizedObjectBehaviorStore,
                           asFieldBehavior: FieldBehavior[A],
                           asParameterBehavior: ParameterBehavior[A],
                           asReturnValueBehavior: ReturnValueBehavior[A]): DefaultSynchronizedObjectBehavior[A] = {
        new DefaultSynchronizedObjectBehavior(classDesc, tree.factory, Option(asFieldBehavior), Option(asParameterBehavior), Option(asReturnValueBehavior))
    }

}
