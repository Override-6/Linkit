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

import fr.linkit.api.connection.packet.persistence.context.TypeProfile
import fr.linkit.api.connection.packet.persistence.obj.InstanceObject
import fr.linkit.engine.connection.packet.persistence.serializor.read.NotInstantiatedObject.Unsafe
import fr.linkit.engine.local.utils.ScalaUtils

class NotInstantiatedObject[T <: AnyRef](override val profile: TypeProfile[T],
                                         content: Array[Int],
                                         reader: PacketReader,
                                         clazz: Class[_]) extends InstanceObject[T] {

    private var isInit: Boolean = false
    private val obj   : T       = Unsafe.allocateInstance(clazz).asInstanceOf[T]

    override def value: T = obj

    def initObject(): Unit = {
        if (isInit)
            return

        isInit = true
        val content = this.content
        val length  = content.length
        val args    = new Array[Any](length)
        var i       = 0
        while (i < length) {
            args(i) = reader.getObject(content(i)) match {
                case o: NotInstantiatedObject[AnyRef] =>
                    o.initObject()
                    o.obj
                case o: InstanceObject[AnyRef]        => o.value
                case o                                => o
            }
            i += 1
        }
        profile.completeInstance(obj, args)
    }

}

object NotInstantiatedObject {

    private val Unsafe = ScalaUtils.findUnsafe()
}
