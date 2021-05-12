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

package fr.linkit.api.connection.cache.repo

import scala.reflect.ClassTag

trait PuppetRepository[A <: Serializable] {

    def getPuppetDescription[B <: A : ClassTag]: PuppetDescription[B]

    def postObject[B <: A : ClassTag](id: Int, obj: B): B with PuppetWrapper[B]

    def findObject[B <: A](id: Int): Option[B with PuppetWrapper[B]]

    def isRegistered(identifier: Int): Boolean

    def initPuppetWrapper[B <: A : ClassTag](wrapper: B with PuppetWrapper[B]): Unit

    def getOrElse[U >: A](id: Int, orElse: => U): U = findObject(id).getOrElse(orElse)
}
