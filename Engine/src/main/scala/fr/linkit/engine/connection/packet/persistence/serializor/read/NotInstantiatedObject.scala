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

package fr.linkit.engine.connection.packet.persistence.serializor.read

import fr.linkit.api.connection.packet.persistence.context.{PersistenceConfig, TypeProfile}
import fr.linkit.api.connection.packet.persistence.obj.{InstanceObject, PoolObject}
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol.Object
import fr.linkit.engine.connection.packet.persistence.serializor.read.NotInstantiatedObject.Unsafe
import fr.linkit.engine.local.utils.{JavaUtils, ScalaUtils}

class NotInstantiatedObject[T <: AnyRef](override val profile: TypeProfile[T],
                                         config: PersistenceConfig,
                                         content: Array[Int],
                                         pool: DeserializerPacketObjectPool) extends InstanceObject[T] {

    private var isInit: Boolean = false
    private val obj   : T       = Unsafe.allocateInstance(profile.typeClass).asInstanceOf[T]
    private val objChunk        = pool.getChunkFromFlag[NotInstantiatedObject[AnyRef]](Object)

    override def value: T = obj

    override def equals(obj: Any): Boolean = JavaUtils.sameInstance(obj, this.obj)

    def initObject(): Unit = {
        if (isInit)
            return

        isInit = true
        val content = this.content
        val length  = content.length
        val args    = new Array[Any](length)
        var i       = 0
        val pool    = this.pool
        while (i < length) {
            args(i) = pool.getAny(content(i)) match {
                case o: NotInstantiatedObject[AnyRef] =>
                    o.initObject()
                    o.obj
                case o: PoolObject[AnyRef]            => o.value
                case o: Array[AnyRef]                 => //TODO create a NotInstantiatedArray
                    initArray(o)
                    o
                case o                                => o
            }
            i += 1
        }
        profile.getPersistence(args).initInstance(obj, args)
        config.informObjectReceived(obj)
    }

    private def initArray(array: Array[AnyRef]): Unit = {
        var n   = 0
        val len = array.length
        while (n < len) {
            val idx = objChunk.indexOf(array(n))
            if (idx != -1)
                objChunk.get(idx).initObject()
            n += 1
        }
    }

}

object NotInstantiatedObject {

    private val Unsafe = ScalaUtils.findUnsafe()
}
