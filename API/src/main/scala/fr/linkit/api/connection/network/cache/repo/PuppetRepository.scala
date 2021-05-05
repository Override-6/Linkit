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

package fr.linkit.api.connection.network.cache.repo

trait PuppetRepository[A <: Serializable] {

    def getPuppetBlueprint(): PuppetDescription[A]

    def postObject(identifier: Int, obj: A): A with PuppetWrapper[A]

    def findObject(identifier: Int): Option[A with PuppetWrapper[A]]

    def isRegistered(identifier: Int): Boolean

    def initPuppetWrapper(wrapper: A with PuppetWrapper[A]): Unit

    def getOrElse[U >: A](id: Int, orElse: => U): U = findObject(id).getOrElse(orElse)
}
