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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypeProfile}
import fr.linkit.api.gnom.persistence.obj.{PoolObject, ProfilePoolObject, RegistrablePoolObject}
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.persistence.obj.ObjectSelector
import fr.linkit.engine.internal.util.{JavaUtils, ScalaUtils}

class NotInstantiatedObject[T <: AnyRef](override val profile: TypeProfile[T],
                                         clazz: Class[_],
                                         content: Array[Int],
                                         controlBox: ControlBox,
                                         selector: ObjectSelector,
                                         pool: DeserializerObjectPool) extends ProfilePoolObject[T] with RegistrablePoolObject[T] {
    
    private var isInit      : Boolean = false
    private var isRegistered: Boolean = false
    private val obj         : T       = ScalaUtils.allocate[T](clazz)
    private val contentObjects        = new Array[RegistrablePoolObject[AnyRef]](content.length)
    
    override def register(): Unit = {
        if (isRegistered)
            return
        isRegistered = true
        obj match {
            case o: NetworkObject[NetworkObjectReference] =>
                selector.handleObject(o)
            case _                                        =>
        }
        var i = 0
        while (i < contentObjects.length) {
            val obj = contentObjects(i)
            if (obj != null)
                obj.register()
            i += 1
        }
    }
    
    override def value: T = {
        if (!isInit)
            buildObject()
        obj
    }
    
    override val identity: Int = System.identityHashCode(obj)
    
    override def equals(obj: Any): Boolean = JavaUtils.sameInstance(obj, this.obj)
    
    private def buildObject(): Unit = {
        isInit = true
        val content = this.content
        val length  = content.length
        val args    = new Array[Any](length)
        var i       = 0
        val pool    = this.pool
        while (i < length) {
            val poolObj = pool.getAny(content(i))
            args(i) = poolObj match {
                case o: RegistrablePoolObject[AnyRef] =>
                    contentObjects(i) = o
                    o.value
                case o: PoolObject[AnyRef]            => o.value
                case o                                => o
            }
            i += 1
        }
        profile.getPersistence(args).initInstance(obj, args, controlBox)
    }
    
}
