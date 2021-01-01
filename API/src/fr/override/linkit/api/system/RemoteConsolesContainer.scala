package fr.`override`.linkit.api.system

import java.util.Collections
import java.util.concurrent.{ConcurrentHashMap, ThreadLocalRandom}

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.RelayException
import fr.`override`.linkit.api.packet.channel.PacketChannel
import fr.`override`.linkit.api.packet.fundamental.{DataPacket, EmptyPacket}
import fr.`override`.linkit.api.system.RemoteConsole
import fr.`override`.linkit.api.system.RemoteConsolesContainer.{AsyncConsolesCollectorID, SyncConsolesCollectorID}

class RemoteConsolesContainer(relay: Relay) {

    private val outConsoles = new ContainerHelper(relay, RemoteConsole.mock(), RemoteConsole.out, "out")
    private val errConsoles = new ContainerHelper(relay, RemoteConsole.mockErr(), RemoteConsole.err, "err")

    private val asyncConsoleCollector = relay.createAsyncCollector(AsyncConsolesCollectorID)
    private val syncConsoleCollector = relay.createSyncCollector(SyncConsolesCollectorID)

    listenRequests()

    def getOut(targetId: String): RemoteConsole = outConsoles.get(targetId)

    def getErr(targetId: String): RemoteConsole.Err = errConsoles.get(targetId)

    private def listenRequests(): Unit = {
        asyncConsoleCollector.onPacketReceived((packet, coos) => {
            val data = packet.asInstanceOf[DataPacket]
            val owner = coos.senderID
            val consoleType = data.header
            val consoleChannelId = new String(data.content).toInt

            val channel = relay.createAsyncChannel(owner, consoleChannelId)

            consoleType match {
                case "out" => outConsoles.add(channel)
                case "err" => errConsoles.add(channel)
            }
            syncConsoleCollector.sendPacket(EmptyPacket, owner)
        })
    }

    class ContainerHelper[T <: RemoteConsole](relay: Relay,
                                              mock: T,
                                              supplier: PacketChannel.Async => T,
                                              name: String) {

        private val consoles = Collections.synchronizedMap(new ConcurrentHashMap[String, T])

        //TODO check if the targeted network is connected
        def get(targetId: String): T = {
            if (!relay.configuration.enableRemoteConsoles)
                return mock

            if (targetId == relay.identifier)
                throw new RelayException("Attempted to get remote console of this current relay")

            if (consoles.containsKey(targetId))
                return consoles.get(targetId)

            val id = ThreadLocalRandom.current().nextInt()
            val channel = relay.createAsyncChannel(targetId, id)
            val console = supplier(channel)

            asyncConsoleCollector.sendPacket(DataPacket(name, id.toString), targetId)
            syncConsoleCollector.nextPacket(targetId)
            consoles.put(targetId, console)

            console
        }

        def add(channel: PacketChannel.Async): Unit =
            consoles.put(channel.connectedID, supplier(channel))
    }

}

object RemoteConsolesContainer {
    val SyncConsolesCollectorID = 7
    val AsyncConsolesCollectorID = 8
}
