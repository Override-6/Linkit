package fr.`override`.linkit.api.connection.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.connection.network.cache.SharedCacheHandler

import java.sql.Timestamp


trait Network {

    val selfEntity: SelfNetworkEntity

    val globalCache: SharedCacheHandler

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def serverEntity: NetworkEntity = getEntity(Relay.ServerIdentifier).get

    def isConnected(identifier: String): Boolean

    def startUpDate: Timestamp

}