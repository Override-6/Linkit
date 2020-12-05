package fr.overridescala.vps.ftp.api.system

import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.vps.ftp.api.Relay

import scala.collection.mutable

class RemoteConsolesHandler(relay: Relay) {

    private val outConsoles = mutable.Map.empty[String, RemoteConsole]
    private val errConsoles = mutable.Map.empty[String, RemoteConsole.Err]

    def getOut(targetId: String, sysChannel: SystemPacketChannel): RemoteConsole = {
        find(targetId, false, outConsoles, sysChannel)
    }

    def getErr(targetId: String, sysChannel: SystemPacketChannel): RemoteConsole.Err = {
        find(targetId, true, errConsoles, sysChannel)
    }

    def linkErr(targetID: String, channelID: Int): Unit = {
        val channel = relay.createAsyncChannel(targetID, channelID)
        val remoteConsole = RemoteConsole.err(channel)
        errConsoles.put(targetID, remoteConsole)
    }

    def linkOut(targetId: String, channelID: Int): Unit = {
        val channel = relay.createAsyncChannel(targetId, channelID)
        val remoteConsole = RemoteConsole.out(channel)
        outConsoles.put(targetId, remoteConsole)
    }

    private def find[T <: RemoteConsole](targetId: String,
                                         isErr: Boolean,
                                         map: mutable.Map[String, T],
                                         systemChannel: SystemPacketChannel): T = {
        if (map.contains(targetId))
            return map(targetId)

        val consoleChannelId = ThreadLocalRandom.current().nextInt()

        val order = if (isErr) SystemOrder.LINK_CONSOLE_ERR else SystemOrder.LINK_CONSOLE_OUT
        systemChannel.sendOrder(order, Reason.INTERNAL, consoleChannelId.toString.getBytes)

        val channel = relay.createAsyncChannel(targetId, consoleChannelId)
        val remoteConsole = (if (isErr) RemoteConsole.err(channel) else RemoteConsole.out(channel)).asInstanceOf[T]
        map.put(targetId, remoteConsole)
        remoteConsole
    }

}

object RemoteConsolesHandler {
    val OutConsoleHandlerChannelID = 7
    val ErrConsoleHandlerChannelID = 8
}
