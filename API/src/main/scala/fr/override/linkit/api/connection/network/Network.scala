package fr.`override`.linkit.api.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager

import java.sql.Timestamp


trait Network {

    val connectionEntity: NetworkEntity

    val globalCache: SharedCacheManager

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def serverEntity: NetworkEntity

    def isConnected(identifier: String): Boolean

    def startUpDate: Timestamp

}