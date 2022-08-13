/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.instantiation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator

case class InstanceWrapper[T <: AnyRef](obj: T with SynchronizedObject[T]) extends SyncInstanceCreator[T] {

    override val syncClassDef: SyncClassDef = SyncClassDef(obj.getSourceClass) //TODO make the object's SyncClassDef accessible.

    override def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T] = {
        //val clazz = obj.getClass
        //if (clazz != syncClass)
        //    throw new IllegalArgumentException(s"Required sync object type is not equals to stored sync object (${obj.getClass} / $syncClass")
        obj
    }

    override def getOrigin: Option[T] = Some(obj)
}
