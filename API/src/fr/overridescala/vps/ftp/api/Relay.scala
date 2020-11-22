package fr.overridescala.vps.ftp.api

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.system.event.EventDispatcher
import fr.overridescala.vps.ftp.api.packet.{PacketChannel, PacketManager}
import fr.overridescala.vps.ftp.api.system.JustifiedCloseable
import fr.overridescala.vps.ftp.api.task.TaskScheduler

//TODO reedit doc about all changes
trait Relay extends JustifiedCloseable with TaskScheduler {

    val identifier: String

    val packetManager: PacketManager

    val extensionLoader: RelayExtensionLoader

    val properties: RelayProperties

    val eventDispatcher: EventDispatcher

    def start(): Unit

    def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync

    def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async

}
