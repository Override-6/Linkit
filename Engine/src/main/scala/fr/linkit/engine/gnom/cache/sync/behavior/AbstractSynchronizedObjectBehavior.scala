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

package fr.linkit.engine.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.behavior.SynchronizedObjectBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.gnom.cache.sync.description.SyncObjectSuperclassDescription

abstract class AbstractSynchronizedObjectBehavior[A <: AnyRef] protected(override val classDesc: SyncObjectSuperclassDescription[A]) extends SynchronizedObjectBehavior[A] {

    protected val methods: Map[Int, InternalMethodBehavior]

    protected val fields: Map[Int, FieldBehavior[AnyRef]]

    override def listMethods(): Iterable[MethodBehavior] = {
        methods.values
    }

    override def listField(): Iterable[FieldBehavior[AnyRef]] = {
        fields.values
    }

    override def getMethodBehavior(id: Int): Option[InternalMethodBehavior] = methods.get(id)

    override def getFieldBehavior(id: Int): Option[FieldBehavior[AnyRef]] = fields.get(id)

}
