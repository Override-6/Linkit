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

package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.connection.cache.obj.description.SyncObjectSuperclassDescription

trait SynchronizedObjectBehavior[A <: AnyRef] extends ValueBehavior[A] {

    val classDesc: SyncObjectSuperclassDescription[A]

    def listMethods(): Iterable[MethodBehavior]

    def getMethodBehavior(id: Int): Option[InternalMethodBehavior]

    def listField(): Iterable[FieldBehavior[Any]]

    def getFieldBehavior(id: Int): Option[FieldBehavior[Any]]

}
