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

package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.network.cache.SharedCacheHandler

import java.sql.Timestamp


trait Network {

    val startUpDate: Timestamp

    val selfEntity: SelfNetworkEntity

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def addOnEntityAdded(action: NetworkEntity => Unit): Unit

    val globalCache: SharedCacheHandler

}