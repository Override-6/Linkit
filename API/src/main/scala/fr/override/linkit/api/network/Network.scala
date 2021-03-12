package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler

import java.sql.Timestamp


trait Network {

    val startUpDate: Timestamp

    val selfEntity: SelfNetworkEntity

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def serverEntity: NetworkEntity = getEntity(Relay.ServerIdentifier).get

    val globalCache: SharedCacheHandler

}