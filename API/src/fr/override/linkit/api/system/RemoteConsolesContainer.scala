package fr.`override`.linkit.api.system

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{RelayException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel}
import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.system.RemoteConsole
import fr.`override`.linkit.api.system.RemoteConsolesContainer.CollectorID

class RemoteConsolesContainer(relay: Relay) {

    private val asyncConsoleMessageCollector = relay.createCollector(CollectorID, AsyncPacketCollector)

    private val outConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole])
    private val errConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole])

    def getOut(targetId: String): RemoteConsole = get(targetId, RemoteConsole.out, outConsoles)

    def getErr(targetId: String): RemoteConsole = get(targetId, RemoteConsole.err, errConsoles)

    private def get(targetId: String, supplier: PacketChannel => RemoteConsole, consoles: util.Map[String, RemoteConsole]): RemoteConsole = {
        if (!relay.configuration.enableRemoteConsoles)
            return RemoteConsole.Mock

        if (targetId == relay.identifier)
            throw new RelayException("Attempted to get remote console of this current relay")

        if (consoles.containsKey(targetId))
            return consoles.get(targetId)

        val channel: PacketChannel = asyncConsoleMessageCollector.subChannel(targetId, AsyncPacketChannel)
        val console = supplier(channel)
        consoles.put(targetId, console)

        console
    }

    init()

    protected def init() {
        asyncConsoleMessageCollector.onPacketInjected((packet, coords) => packet match {
            case data: DataPacket =>
                val output = if (data.header == "err") System.err else System.out
                output.println(s"[${coords.senderID}]: ${data.contentAsString}")
            case other: Packet => throw new UnexpectedPacketException(s"Unexpected packet '${other.getClass.getName}' injected in a remote console.")
        })
    }

}

object RemoteConsolesContainer {
    val CollectorID = 8
}