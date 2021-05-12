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

import fr.linkit.engine.connection.cache.repo.ObjectChip

import scala.collection.mutable.ListBuffer

class PuppetObject[S <: Serializable](chip: ObjectChip[S]) extends SyncNode {

    /**
     * This seq contains all the fields synchronized of the object
     * */
    private val fields: ListBuffer[SyncNode] = ListBuffer.empty

    /**
     * This seq contains other synchronized objects such as return types and parameters
     * */
    private val atmosphere: ListBuffer[SyncNode] = ListBuffer.empty

    override def getChildren: Seq[SyncNode] = fields.toSeq ++ atmosphere.toSeq

    override def handleBundle(bundle: TreeRequestBundle): Unit = {
        puppeteer
    }
}
