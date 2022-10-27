/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.network

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.referencing.StaticNetworkObject
import fr.linkit.api.gnom.referencing.linker.GeneralNetworkObjectLinker

import java.sql.Timestamp

trait Network extends StaticNetworkObject[NetworkReference.type] {

    val connection: ConnectionContext

    val gnol: GeneralNetworkObjectLinker

    def currentEngine: Engine

    def globalCaches: SharedCacheManager

    def serverName: String

    def serverEngine: Engine

    def countConnections: Int

    def listEngines: List[Engine]

    def findEngine(identifier: IdentifierTag): Option[Engine]

    def findEngines(group: GroupTag): List[Engine]

    def startUpDate: Timestamp

    def findCacheManager(family: String): Option[SharedCacheManager]

    def getStaticAccess(id: Int): StaticAccess

    def newStaticAccess(id: Int, contract: ContractDescriptorData): StaticAccess

    def newStaticAccess(id: Int): StaticAccess

    def onNewEngine(f: Engine => Unit): Unit
}