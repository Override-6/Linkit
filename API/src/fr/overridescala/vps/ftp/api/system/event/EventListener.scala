package fr.overridescala.vps.ftp.api.system.event

import fr.overridescala.vps.ftp.api.`extension`.RelayExtension
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel, PacketCoordinates}
import fr.overridescala.vps.ftp.api.system.{Reason, SystemOrder}
import fr.overridescala.vps.ftp.api.task.Task

abstract class EventListener {

    def onReady(): Unit = ()

    def onConnected(): Unit = ()

    def onDisconnected(): Unit = ()

    def onClosed(relayId: String, reason: Reason): Unit = ()

    def onTaskScheduled(task: Task[_]): Unit = ()

    def onTaskSkipped(task: Task[_], reason: Reason): Unit = ()

    def onTaskStartExecuting(task: Task[_]): Unit = ()

    def onTaskEnd(task: Task[_], reason: Reason): Unit = ()

    def onExtensionLoaded(extension: RelayExtension): Unit = ()

    def onPacketTypeRegistered[T <: Packet](packetClass: Class[T], factoryClass: PacketFactory[T]): Unit = ()

    def onPacketChannelRegistered(channel: PacketChannel): Unit = ()

    def onPacketChannelUnregistered(channel: PacketChannel, reason: Reason): Unit = ()

    def onPacketSent(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketReceived(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onSystemOrderReceived(orderType: SystemOrder, reason: Reason): Unit = ()

    def onSystemOrderSent(orderType: SystemOrder): Unit = ()

}
