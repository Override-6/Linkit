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

import fr.linkit.api.connection.cache.obj.behavior.{FieldBehavior, MemberBehaviorFactory, MethodBehavior, ObjectTreeBehavior, WrapperBehavior}
import fr.linkit.api.local.generation.PuppetClassDescription

class DefaultWrapperBehavior[A] protected(override val classDesc: PuppetClassDescription[A],
                                          factory: MemberBehaviorFactory) extends WrapperBehavior[A] {

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

object DefaultWrapperBehavior {

    def apply[A](classDesc: PuppetClassDescription[A], tree: ObjectTreeBehavior): DefaultWrapperBehavior[A] = {
        new DefaultWrapperBehavior(classDesc, tree.factory)
    }

}
