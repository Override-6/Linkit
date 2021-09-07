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

package fr.linkit.engine.connection.packet.persistence.obj

import fr.linkit.api.connection.packet.persistence.context.TypeProfile
import fr.linkit.engine.connection.packet.persistence.obj.NotInstantiatedObject.Unsafe
import fr.linkit.engine.local.utils.ScalaUtils

class NotInstantiatedObject[T](profile: TypeProfile[T], args: Array[Any], clazz: Class[_]) extends InstanceObject[T] {

    private var isInit  : Boolean = false
    private lazy val obj: T       = Unsafe.allocateInstance(clazz).asInstanceOf[T]

    override def instance: T = obj

    def initObject(): Unit = {
        if (isInit)
            return

        val length = this.args.length
        val args   = new Array[Any](length)
        for (i <- 0 to length)
            args(i) = this.args(i) match {
                case o: NotInstantiatedObject[Any] =>
                    o.initObject()
                    o.instance
                case o: InstanceObject[Any]        => o.instance
                case o                             => o
            }
        profile.completeInstance(obj, args)
        isInit = true
    }

}

object NotInstantiatedObject {

    private val Unsafe = ScalaUtils.findUnsafe()
}
