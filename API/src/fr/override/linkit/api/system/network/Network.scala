package fr.`override`.linkit.api.system.network

import java.sql.Timestamp


trait Network {

    val onlineTimeStamp: Timestamp

    def listEntities: List[NetworkEntity]

    def getEntity(identifier: String): Option[NetworkEntity]

    def setOnEntityAdded(action: NetworkEntity => Unit): Unit


}