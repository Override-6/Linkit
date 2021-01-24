package fr.`override`.linkit.api.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.cache.SharedCacheHandler


trait Network {

    val startUpDate: Timestamp

    val selfEntity: SelfNetworkEntity

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def addOnEntityAdded(action: NetworkEntity => Unit): Unit

    val globalCache: SharedCacheHandler

}