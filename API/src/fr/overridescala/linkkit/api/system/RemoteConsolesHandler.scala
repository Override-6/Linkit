package fr.overridescala.linkkit.api.system

import java.util.concurrent.ThreadLocalRandom

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.packet.fundamental.DataPacket
import fr.overridescala.linkkit.api.system.RemoteConsolesHandler.ConsolesCollectorID

import scala.collection.mutable

class RemoteConsolesHandler(relay: Relay) {

    private val outConsoles = mutable.LinkedHashMap.empty[String, RemoteConsole]
    private val errConsoles = mutable.LinkedHashMap.empty[String, RemoteConsole.Err]

    private val consoleCollector = relay.createAsyncCollector(ConsolesCollectorID)

    consoleCollector.onPacketReceived((packet, coos) => {
        println("EL PACKETTO ESTAMOS " + packet + ", " + coos)
        val data = packet.asInstanceOf[DataPacket]
        val owner = coos.senderID
        val consoleType = data.header
        val consoleChannelId = new String(data.content).toInt
        val channel = relay.createAsyncChannel(coos.senderID, consoleChannelId)

        println("b")

        consoleType match {
            case "out" => outConsoles.put(owner, RemoteConsole.out(channel))
            case "err" => errConsoles.put(owner, RemoteConsole.err(channel))
        }
        println(s"outConsoles = ${outConsoles}")
        println(s"errConsoles = ${errConsoles}")
    })

    def getOut(targetId: String): RemoteConsole = {
        if (outConsoles.contains(targetId))
            return errConsoles(targetId)

        val consoleChannelId = ThreadLocalRandom.current().nextInt()
        consoleCollector.sendPacket(DataPacket("out", consoleChannelId.toString), targetId)

        val channel = relay.createAsyncChannel(targetId, consoleChannelId)
        val remoteConsole = RemoteConsole.out(channel)
        outConsoles.put(targetId, remoteConsole)
        remoteConsole
    }

    def getErr(targetId: String): RemoteConsole.Err = {
        if (errConsoles.contains(targetId))
            return errConsoles(targetId)

        val consoleChannelId = ThreadLocalRandom.current().nextInt()
        consoleCollector.sendPacket(DataPacket("err", consoleChannelId.toString), targetId)

        val channel = relay.createAsyncChannel(targetId, consoleChannelId)
        val remoteConsole = RemoteConsole.err(channel)
        errConsoles.put(targetId, remoteConsole)
        remoteConsole
    }

    def linkErr(targetID: String, identifier: Int): Unit = {
        val channel = relay.createAsyncChannel(targetID, identifier)
        val remoteConsole = RemoteConsole.err(channel)
        errConsoles.put(targetID, remoteConsole)
    }

    def linkOut(targetId: String, identifier: Int): Unit = {
        val channel = relay.createAsyncChannel(targetId, identifier)
        val remoteConsole = RemoteConsole.out(channel)
        outConsoles.put(targetId, remoteConsole)
    }


}

object RemoteConsolesHandler {
    val ConsolesCollectorID = 7
}
