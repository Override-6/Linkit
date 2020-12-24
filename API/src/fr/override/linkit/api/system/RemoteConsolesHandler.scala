package fr.`override`.linkit.api.system

import java.util
import java.util.Collections
import java.util.concurrent.{ConcurrentHashMap, ThreadLocalRandom}

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.RelayException
import fr.`override`.linkit.api.packet.fundamental.{DataPacket, EmptyPacket}
import fr.`override`.linkit.api.system.RemoteConsolesHandler.{AsyncConsolesCollectorID, SyncConsolesCollectorID}

class RemoteConsolesHandler(relay: Relay) {

    private val outConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole])
    private val errConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, RemoteConsole.Err])

    private val asyncConsoleCollector = relay.createAsyncCollector(AsyncConsolesCollectorID)
    private val syncConsoleCollector = relay.createSyncCollector(SyncConsolesCollectorID)

    if (relay.configuration.enableRemoteConsoles)
        listenRequests()

    def getOut(targetId: String): RemoteConsole = {
        if (!relay.configuration.enableRemoteConsoles)
            return RemoteConsole.mock()

        val remote = getInCache(outConsoles, targetId)
        if (remote.isDefined) // a value has been returned from the checks
            return remote.get

        if (!relay.isConnected(targetId))
            throw new RelayException(s"'$targetId' is not connected on this network")

        val outID = ThreadLocalRandom.current().nextInt()
        val outChannel = relay.createAsyncChannel(targetId, outID)
        val out = RemoteConsole.out(outChannel)
        asyncConsoleCollector.sendPacket(DataPacket("out", outID.toString), targetId)
        syncConsoleCollector.nextPacket(targetId)
        outConsoles.put(targetId, out)

        out
    }

    def getErr(targetId: String): RemoteConsole.Err = {
        if (!relay.configuration.enableRemoteConsoles)
            return RemoteConsole.mockErr()

        val remote = getInCache(errConsoles, targetId)
        if (remote.isDefined) // a value has been returned from the checks
            return remote.get

        if (!relay.isConnected(targetId))
            throw new RelayException(s"'$targetId' is not connected on this network")

        val errID = ThreadLocalRandom.current().nextInt()
        val errChannel = relay.createAsyncChannel(targetId, errID)
        val err = RemoteConsole.err(errChannel)
        asyncConsoleCollector.sendPacket(DataPacket("err", errID.toString), targetId)
        syncConsoleCollector.nextPacket(targetId)
        errConsoles.put(targetId, err)

        err
    }

    private def getInCache[C <: RemoteConsole](cache: util.Map[String, C], targetId: String): Option[C] = {
        if (targetId == relay.identifier)
            throw new RelayException("Attempted to get remote console of this relay")

        if (cache.containsKey(targetId)) {
            return Option(cache.get(targetId))
        }
        Option.empty
    }

    private def listenRequests(): Unit = {
        asyncConsoleCollector.onPacketReceived((packet, coos) => {
            val data = packet.asInstanceOf[DataPacket]
            val owner = coos.senderID
            val consoleType = data.header
            val consoleChannelId = new String(data.content).toInt

            val channel = relay.createAsyncChannel(coos.senderID, consoleChannelId)

            consoleType match {
                case "out" => outConsoles.put(owner, RemoteConsole.out(channel))
                case "err" => errConsoles.put(owner, RemoteConsole.err(channel))
            }
            syncConsoleCollector.sendPacket(EmptyPacket, owner)
        })
    }
}

object RemoteConsolesHandler {
    val SyncConsolesCollectorID = 7
    val AsyncConsolesCollectorID = 8
}
