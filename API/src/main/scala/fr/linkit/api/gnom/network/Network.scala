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

package fr.linkit.api.gnom.network

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.reference.linker.GeneralNetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference, StaticNetworkObject}

import java.sql.Timestamp

trait Network extends StaticNetworkObject[NetworkReference] {

    val connection: ConnectionContext

    val objectManagementChannel: ObjectManagementChannel //TODO remove !!!

    val gnol: GeneralNetworkObjectLinker

    def connectionEngine: Engine

    def globalCache: SharedCacheManager

    def serverIdentifier: String

    def serverEngine: Engine

    def countConnections: Int

    def listEngines: List[Engine]

    def findEngine(identifier: String): Option[Engine]

    def isConnected(identifier: String): Boolean

    def startUpDate: Timestamp

    def getCacheManager(family: String): SharedCacheManager

    def declareNewCacheManager(family: String): SharedCacheManager

    def findCacheManager(family: String): Option[SharedCacheManager]

}