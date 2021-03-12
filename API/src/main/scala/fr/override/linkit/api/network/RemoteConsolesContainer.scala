package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.Relay.Log
import fr.`override`.linkit.api.exception.{RelayException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.fundamental.TaggedObjectPacket
import fr.`override`.linkit.api.packet.traffic.channel.AsyncPacketChannel
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.system.evente.network.NetworkEvents

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class RemoteConsolesContainer(relay: Relay) {

    private val printChannel = relay.getInjectable(PacketTraffic.RemoteConsoles, ChannelScope.broadcast, AsyncPacketChannel)

    private val outConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole])
    private val errConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole])

    def getOut(targetId: String): RemoteConsole = get(targetId, RemoteConsole.out, outConsoles)

    def getErr(targetId: String): RemoteConsole = get(targetId, RemoteConsole.err, errConsoles)

    private def get(targetId: String, supplier: (AsyncPacketChannel, Relay, String) => RemoteConsole, consoles: util.Map[String, RemoteConsole]): RemoteConsole = {
        if (!relay.configuration.enableRemoteConsoles)
            return RemoteConsole.Mock

        if (targetId == relay.identifier)
            throw new RelayException("Attempted to get remote console of this current relay")

        if (consoles.containsKey(targetId))
            return consoles.get(targetId)

        val channel = printChannel.subInjectable(targetId, AsyncPacketChannel, true)
        val console = supplier(channel, relay, targetId)
        consoles.put(targetId, console)

        console
    }

    init()

    protected def init() {
        printChannel.addOnPacketReceived((packet, coords) => {
            packet match {
                case TaggedObjectPacket(header, msg: String) =>
                    val log: String => Unit = if (header == "err") Log.error else Log.info
                    log(s"[${coords.senderID}]: $msg")

                    import relay.networkHooks
                    val entity = relay.network.getEntity(coords.senderID).get
                    val event = NetworkEvents.remotePrintReceivedEvent(entity, msg)
                    relay.eventNotifier.notifyEvent(event)
                case other: Packet => throw new UnexpectedPacketException(s"Unexpected packet '${other.getClass.getName}' injected in a remote console.")
            }
        })
    }

}
