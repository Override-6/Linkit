package fr.overridescala.vps.ftp.api

import fr.overridescala.vps.ftp.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.overridescala.vps.ftp.api.packet.{PacketChannel, PacketManager}
import fr.overridescala.vps.ftp.api.system.event.EventDispatcher
import fr.overridescala.vps.ftp.api.system.{JustifiedCloseable, RemoteConsole, Version}
import fr.overridescala.vps.ftp.api.task.TaskScheduler
import org.jetbrains.annotations.Nullable

//TODO reedit doc about all changes
trait Relay extends JustifiedCloseable with TaskScheduler {

    final val apiVersion = Version("Api", "0.1.1", stable = false)

    val relayVersion: Version

    val identifier: String

    val packetManager: PacketManager

    val extensionLoader: RelayExtensionLoader

    val properties: RelayProperties

    val eventDispatcher: EventDispatcher

    def start(): Unit

    def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync

    def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async

    def getConsoleOut(@Nullable targetId: String): Option[RemoteConsole]

    def getConsoleErr(@Nullable targetId: String): Option[RemoteConsole.Err]

}
