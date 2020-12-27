package fr.`override`.linkit.api.system.network

import java.sql.Timestamp
import java.time.Instant

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.{HoleyPacketContainer, Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.Tuple3Packet
import fr.`override`.linkit.api.utils.Tuple3Packet._

import scala.collection.mutable

abstract class AbstractNetwork(relay: Relay) extends Network {

    private val entities = mutable.Map[String, NetworkEntity]((relay.identifier, new SelfNetworkEntity(relay)))

    override val onlineTimeStamp: Timestamp = Timestamp.from(Instant.now())

    @volatile private var onEntityAdded: NetworkEntity => Unit = _ => ()

    //immutable
    override def listEntities: List[NetworkEntity] = entities.values.to(List)


    override def getEntity(identifier: String): Option[NetworkEntity] = entities.get(identifier)

    override def setOnEntityAdded(action: NetworkEntity => Unit): Unit = onEntityAdded = action

    getAsyncChannel.onPacketReceived((packet, coords) => {
        val tuple = packet.asInstanceOf[Tuple3Packet]
        val order = tuple._1

        if (!handleOrder(tuple)) {
            order match {
                case "add" =>
                    val affected = tuple._2
                    addEntity(affected)


                case "getProperty" =>
                    val name = tuple._2
                    sendPacket(DataPacket("", relay.properties.get(name).getOrElse("")), coords.reversed())
                case "setProperty" =>
                    val name = tuple._2
                    val value = tuple._3
                    relay.properties.putProperty(name, value)

                case "update" =>
                    val entity = getEntity(tuple._2)
                    if (entity.isDefined)
                        updateEntityState(entity.get, ConnectionState.valueOf(tuple._3))
                case "versions" =>
                    //TODO move this request to a System order
                    sendPacket((Relay.ApiVersion.toString, relay.relayVersion.toString), coords.reversed())

                case _ => throw new UnexpectedPacketException(s"Could not handle network order '$order'")
            }
        }
    })

    protected def addEntity(identifier: String): Unit = {
        addEntity(createEntity(identifier))
    }

    protected def addEntity(entity: NetworkEntity): Unit = {
        entities.put(entity.identifier, entity)
        onEntityAdded(entity)
    }

    protected def removeEntity(identifier: String): Unit = {
        entities.remove(identifier)
    }

    protected def createEntity(identifier: String): NetworkEntity

    protected def updateEntityState(entity: NetworkEntity, state: ConnectionState): Unit

    protected def handleOrder(packet: Tuple3Packet): Boolean = false

    protected def sendPacket(packet: Packet, coords: PacketCoordinates): Unit

    protected def getAsyncChannel: HoleyPacketContainer


}
