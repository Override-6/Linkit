package fr.`override`.linkit.api.network

import java.sql.Timestamp


trait Network {

    val onlineTimeStamp: Timestamp

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def addOnEntityAdded(action: NetworkEntity => Unit): Unit


}