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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.persistence.context.TypeProfile
import fr.linkit.api.gnom.persistence.obj.{InstanceObject, PoolObject}
import fr.linkit.api.gnom.reference.NetworkObject
import fr.linkit.engine.gnom.persistence.obj.ObjectSelector
import fr.linkit.engine.internal.utils.{JavaUtils, ScalaUtils}

class NotInstantiatedObject[T <: AnyRef](override val profile: TypeProfile[T],
                                         clazz: Class[_],
                                         content: Array[Int],
                                         selector: ObjectSelector,
                                         pool: DeserializerObjectPool) extends InstanceObject[T] {

    private var isInit: Boolean = false
    private val obj   : T       = ScalaUtils.allocate[T](clazz)

    override def value: T = {
        if (!isInit)
            initObject()
        obj
    }

    override def equals(obj: Any): Boolean = JavaUtils.sameInstance(obj, this.obj)

    private def initObject(): Unit = {
        isInit = true
        val content = this.content
        val length  = content.length
        val args    = new Array[Any](length)
        var i       = 0
        val pool    = this.pool
        while (i < length) {
            args(i) = pool.getAny(content(i)) match {
                case o: PoolObject[AnyRef] => o.value
                case o                     => o
            }
            i += 1
        }
        profile.getPersistence(args).initInstance(obj, args)
        obj match {
            case syncObj: NetworkObject[SharedCacheManagerReference]  =>
                selector.initObject(syncObj)
            case _ =>
        }
    }

}
