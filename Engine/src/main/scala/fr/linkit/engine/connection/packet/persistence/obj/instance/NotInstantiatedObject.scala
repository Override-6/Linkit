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

package fr.linkit.engine.connection.packet.persistence.obj.instance

import fr.linkit.api.connection.packet.persistence.context.TypeProfile
import fr.linkit.engine.connection.packet.persistence.obj.instance.NotInstantiatedObject.Unsafe
import fr.linkit.engine.connection.packet.persistence.serializor.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.serializor.read.ObjectPoolReader
import fr.linkit.engine.local.utils.ScalaUtils

class NotInstantiatedObject[T](profile: TypeProfile[T], content: Array[Char], reader: ObjectPoolReader, clazz: Class[_]) extends InstanceObject[T] {

    private var isInit: Boolean = false
    private val obj   : T       = Unsafe.allocateInstance(clazz).asInstanceOf[T]

    override def instance: T = obj

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
                case o: NotInstantiatedObject[Any] =>
                    o.initObject()
                    o.obj
                case o: InstanceObject[Any]        => o.instance
                case o                             => o
            }
            i += 1
        }
        profile.completeInstance(obj, args)
    }

}

object NotInstantiatedObject {

    private val Unsafe = ScalaUtils.findUnsafe()
}
