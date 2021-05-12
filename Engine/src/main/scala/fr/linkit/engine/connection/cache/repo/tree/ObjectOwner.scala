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

package fr.linkit.engine.connection.cache.repo.tree

import fr.linkit.engine.connection.packet.traffic.channel.request.RequestBundle

import scala.collection.mutable.ListBuffer

class ObjectOwner extends SyncNode {

    private val children: ListBuffer[SyncNode] = ListBuffer.empty

    override def getChildren: Seq[SyncNode] = children.toSeq

    override def handleBundle(bundle: RequestBundle): Unit = {

    }
}
