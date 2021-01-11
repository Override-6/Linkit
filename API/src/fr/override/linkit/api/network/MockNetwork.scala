package fr.`override`.linkit.api.network
import java.sql.Timestamp

class MockNetwork extends Network {
    override val onlineTimeStamp: Timestamp = new Timestamp(System.currentTimeMillis())

    override def listEntities: List[NetworkEntity] = List.empty

    override def getEntity(identifier: String): Option[NetworkEntity] = None

    override def addOnEntityAdded(action: NetworkEntity => Unit): Unit = ()
}
