/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.`extension`

import scala.collection.mutable

class RelayProperties {

    private val map: mutable.Map[String, Any] = mutable.Map.empty

    def putProperty(key: String, value: Any): Any = {
        map.put(key, value).orNull
    }

    def get[T](key: String): Option[T] = {
        map.get(key).asInstanceOf[Option[T]]
    }

    def getProperty[T](key: String): T = {
        val opt = get(key)
        if (opt.isEmpty)
            throw new NoSuchElementException(s"value '$key' is not set into this relay properties")
        opt.get
    }

    def foreach(action: (String, Any) => Unit): Unit = {
        map.foreach(element => action(element._1, element._2))
    }

}
