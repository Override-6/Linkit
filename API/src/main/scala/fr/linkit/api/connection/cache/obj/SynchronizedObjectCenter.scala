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

package fr.linkit.api.connection.cache.obj

import fr.linkit.api.connection.cache.SharedCache
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.tree.ObjectTreeCenter
import fr.linkit.api.connection.packet.PacketAttributesPresence

trait SynchronizedObjectCenter[A <: AnyRef] extends PacketAttributesPresence with SharedCache {

    val treeCenter: ObjectTreeCenter[A]

    val defaultTreeViewBehavior: ObjectTreeBehavior

    def postObject(id: Int, obj: A): A with PuppetWrapper[A]

    def postObject(id: Int, obj: A, behavior: ObjectTreeBehavior): A with PuppetWrapper[A]

    def findObject(id: Int): Option[A with PuppetWrapper[A]]

    def getOrPost(id: Int, orPost: => A): A = findObject(id).getOrElse(postObject(id, orPost))

    def isRegistered(id: Int): Boolean

}
