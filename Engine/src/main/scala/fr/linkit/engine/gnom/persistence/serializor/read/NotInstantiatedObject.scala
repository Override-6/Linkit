/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.context.TypeProfile
import fr.linkit.api.gnom.persistence.obj.{InstanceObject, PoolObject}
import fr.linkit.api.gnom.reference.{NetworkObjectLinker, NetworkObjectReference}
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol.Object
import fr.linkit.engine.internal.utils.{JavaUtils, ScalaUtils}

class NotInstantiatedObject[T <: AnyRef](override val profile: TypeProfile[T],
                                         gnol: NetworkObjectLinker,
                                         content: Array[Int],
                                         pool: DeserializerObjectPool) extends InstanceObject[T] {

    private var isInit: Boolean = false
    private val obj   : T       = ScalaUtils.allocate[T](profile.typeClass)
    private val objChunk        = pool.getChunkFromFlag[NotInstantiatedObject[AnyRef]](Object)

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
                case o: Array[AnyRef]      =>
                    initArray(o)
                    o
                case o                     => o
            }
            i += 1
        }
        profile.getPersistence(args).initInstance(obj, args)
        //config.informObjectReceived(obj)
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
