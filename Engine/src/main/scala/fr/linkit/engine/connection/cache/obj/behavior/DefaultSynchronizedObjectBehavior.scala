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

import fr.linkit.api.connection.cache.obj.behavior.member.{FieldBehavior, MemberBehaviorFactory, MethodBehavior}
import fr.linkit.api.connection.cache.obj.behavior.{SynchronizedObjectBehavior, SynchronizedObjectBehaviorStore}
import fr.linkit.api.connection.cache.obj.description.SyncObjectSuperclassDescription

class DefaultSynchronizedObjectBehavior[A] protected(override val classDesc: SyncObjectSuperclassDescription[A],
                                                     factory: MemberBehaviorFactory) extends SynchronizedObjectBehavior[A] {

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

    override def getMethodBehavior(id: Int): Option[MethodBehavior] = methods.get(id)

    override def listField(): Iterable[FieldBehavior] = {
        fields.values
    }

    override def getFieldBehavior(id: Int): Option[FieldBehavior] = fields.get(id)

    protected def generateMethodsBehavior(): Iterable[MethodBehavior] = {
        classDesc.listMethods()
                .map(factory.genMethodBehavior(None, _))
    }

    protected def generateFieldsBehavior(): Iterable[FieldBehavior] = {
        classDesc.listFields()
                .map(factory.genFieldBehavior)
    }

}

object DefaultSynchronizedObjectBehavior {

    def apply[A](classDesc: SyncObjectSuperclassDescription[A], tree: SynchronizedObjectBehaviorStore): DefaultSynchronizedObjectBehavior[A] = {
        new DefaultSynchronizedObjectBehavior(classDesc, tree.factory)
    }

}
