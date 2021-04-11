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

package fr.linkit.api.connection.network

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.{ConnectionContext, ExternalConnection}

import java.sql.Timestamp

trait Network {

    val connectionEntity: NetworkEntity

    val globalCache: SharedCacheManager

    val connection: ConnectionContext

    def serverIdentifier: String

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def serverEntity: NetworkEntity

    def isConnected(identifier: String): Boolean

    def startUpDate: Timestamp

    def newCacheManager(family: String, owner: ConnectionContext): SharedCacheManager

    def newCacheManager(family: String, owner: ExternalConnection): SharedCacheManager

}