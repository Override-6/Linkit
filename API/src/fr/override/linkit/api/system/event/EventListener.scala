package fr.`override`.linkit.api.system.event

import fr.`override`.linkit.api.`extension`.RelayExtension
import fr.`override`.linkit.api.packet.{Packet, PacketContainer, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, SystemOrder}
import fr.`override`.linkit.api.`extension`.RelayExtension
import fr.`override`.linkit.api.`extension`.packet.PacketFactory
import fr.`override`.linkit.api.packet.{Packet, PacketContainer, PacketCoordinates}
import fr.`override`.linkit.api.system.{CloseReason, SystemOrder}
import fr.`override`.linkit.api.task.Task

@deprecated
abstract class EventListener {

    def onReady(): Unit = ()

    def onConnected(): Unit = ()

    def onDisconnected(): Unit = ()

    def onClosed(relayId: String, reason: CloseReason): Unit = ()

    def onTaskScheduled(task: Task[_]): Unit = ()

    def onTaskSkipped(task: Task[_], reason: CloseReason): Unit = ()

    def onTaskStartExecuting(task: Task[_]): Unit = ()

    def onTaskEnd(task: Task[_], reason: CloseReason): Unit = ()

    def onExtensionLoaded(extension: RelayExtension): Unit = ()

    def onPacketTypeRegistered[T <: Packet](packetClass: Class[T], factoryClass: PacketFactory[T]): Unit = ()

    def onPacketContainerRegistered(container: PacketContainer): Unit = ()

    def onPacketContainerUnregistered(container: PacketContainer, reason: CloseReason): Unit = ()

    def onPacketSent(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketReceived(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit = ()

    def onSystemOrderReceived(orderType: SystemOrder, reason: CloseReason): Unit = ()

    def onSystemOrderSent(orderType: SystemOrder): Unit = ()

}
