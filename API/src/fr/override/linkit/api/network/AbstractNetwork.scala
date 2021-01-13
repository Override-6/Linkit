package fr.`override`.linkit.api.network
import java.sql.Timestamp

import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.utils.cache.{SharedCaches, SharedInstance}

import scala.collection.mutable.ListBuffer

abstract class AbstractNetwork(implicit traffic: PacketTraffic) extends Network {
    override val onlineTimeStamp: Timestamp = new SharedInstance[Timestamp](collector, false).get //will synchronises withe the Server
    private val entities: ListBuffer[NetworkEntity] = SharedCaches.createCollection(4)

    override def listEntities: List[NetworkEntity] = List.empty

    override def getEntity(identifier: String): Option[NetworkEntity] = None

    override def addOnEntityAdded(action: NetworkEntity => Unit): Unit = ()
}
