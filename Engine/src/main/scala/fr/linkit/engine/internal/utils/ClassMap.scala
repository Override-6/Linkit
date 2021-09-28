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

package fr.linkit.engine.internal.utils

import scala.collection.mutable

class ClassMap[V] extends mutable.HashMap[Class[_], V]() {

    def this(other: ClassMap[V]) {
        this()
        addAll(other.iterator)
    }

    def this(other: Map[Class[_], V]) {
        this()
        addAll(other.iterator)
    }

    override def get(key: Class[_]): Option[V] = {
        val opt = super.get(key)
        if (opt.isDefined)
            opt
        else getFirstSuper(key)
    }

    private def getFirstSuper(key: Class[_]): Option[V] = {
        if (key == null)
            return None
        for (interface <- key.getInterfaces) {
            val opt = get(interface)
            if (opt.isDefined)
                return opt
        }
        get(key.getSuperclass)
    }

}
