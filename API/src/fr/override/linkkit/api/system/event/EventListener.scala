package fr.`override`.linkkit.api.system.event

import fr.`override`.linkkit.api.`extension`.RelayExtension
import fr.`override`.linkkit.api.`extension`.packet.PacketFactory
import fr.`override`.linkkit.api.packet.{Packet, PacketContainer, PacketCoordinates}
import fr.`override`.linkkit.api.system.{Reason, SystemOrder}
import fr.`override`.linkkit.api.task.Task

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
