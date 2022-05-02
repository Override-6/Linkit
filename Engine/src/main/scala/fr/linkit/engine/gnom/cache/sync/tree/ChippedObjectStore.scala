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

package fr.linkit.engine.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.ChippedObject

import scala.collection.mutable
import scala.util.Try

object ChippedObjectStore {

    private val map = mutable.HashMap.empty[Any, ChippedObject[_]]

    private[tree] def addChippedObject(chippedObject: ChippedObject[_]): Unit = {
        map.put(chippedObject.connected, chippedObject)
    }

    def findConnectedObject(obj: Any): Option[ChippedObject[_]] = {
        if (obj == null) return None
        Try(map.get(obj)).toOption.flatten
    }

}
