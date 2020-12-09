package fr.overridescala.linkkit.api.system.event

import fr.overridescala.linkkit.api.`extension`.RelayExtension
import fr.overridescala.linkkit.api.`extension`.packet.PacketFactory
import fr.overridescala.linkkit.api.packet.{Packet, PacketContainer, PacketCoordinates}
import fr.overridescala.linkkit.api.system.{Reason, SystemOrder}
import fr.overridescala.linkkit.api.task.Task

@deprecated
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

    def onPacketContainerRegistered(container: PacketContainer): Unit = ()

    def onPacketContainerUnregistered(container: PacketContainer, reason: Reason): Unit = ()

    def onPacketSent(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketReceived(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onSystemOrderReceived(orderType: SystemOrder, reason: Reason): Unit = ()

    def onSystemOrderSent(orderType: SystemOrder): Unit = ()

}
