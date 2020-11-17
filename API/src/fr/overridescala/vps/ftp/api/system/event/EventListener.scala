package fr.overridescala.vps.ftp.api.system.event

import fr.overridescala.vps.ftp.api.`extension`.RelayExtension
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannelManager}
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

    def onPacketChannelRegistered(channel: PacketChannelManager): Unit = ()

    def onPacketChannelUnregistered(channel: PacketChannelManager, reason: Reason): Unit = ()

    def onPacketSent(packet: Packet): Unit = ()

    def onPacketReceived(packet: Packet): Unit = ()

    def onPacketUsed(packet: Packet): Unit = ()

    def onSystemOrderReceived(orderType: SystemOrder, reason: Reason): Unit = ()

    def onSystemOrderSent(orderType: SystemOrder): Unit = ()

}
