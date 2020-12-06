package fr.overridescala.linkkit.api.system.event

import fr.overridescala.linkkit.api.`extension`.RelayExtension
import fr.overridescala.linkkit.api.system.event.EventObserver.EventNotifier
import fr.overridescala.linkkit.api.`extension`.packet.PacketFactory
import fr.overridescala.linkkit.api.packet.{Packet, PacketChannel, PacketCoordinates}
import fr.overridescala.linkkit.api.system.{Reason, SystemOrder}
import fr.overridescala.linkkit.api.task.Task
import javax.management.InstanceAlreadyExistsException

import scala.collection.mutable.ListBuffer

class EventObserver {

    val notifier = new EventNotifier

    def register(listener: EventListener): Unit = {
        if (listener == null)
            throw new NullPointerException("Listener is null !")
        if (notifier.listeners.contains(listener))
            throw new InstanceAlreadyExistsException("listener is already registered !")
        notifier.listeners += listener
    }

}

object EventObserver {
    class EventNotifier {
        private[EventObserver] val listeners = ListBuffer.empty[EventListener]

        def onReady(): Unit = dispatch(_.onReady())

        def onConnected(): Unit = dispatch(_.onConnected())

        def onDisconnected(): Unit = dispatch(_.onDisconnected())

        def onClosed(relayId: String, reason: Reason): Unit = dispatch(_.onClosed(relayId, reason))

        def onTaskScheduled(task: Task[_]): Unit = dispatch(_.onTaskScheduled(task))

        def onTaskSkipped(task: Task[_], reason: Reason): Unit = dispatch(_.onTaskSkipped(task, reason))

        def onTaskStartExecuting(task: Task[_]): Unit = dispatch(_.onTaskStartExecuting(task))

        def onTaskEnd(task: Task[_], reason: Reason): Unit = dispatch(_.onTaskEnd(task, reason))

        def onExtensionLoaded(extension: RelayExtension): Unit = dispatch(_.onExtensionLoaded(extension))

        def onPacketTypeRegistered[T <: Packet](packetClass: Class[T], factoryClass: PacketFactory[T]): Unit = dispatch(_.onPacketTypeRegistered(packetClass, factoryClass))

        def onPacketChannelRegistered(channel: PacketChannel): Unit = dispatch(_.onPacketChannelRegistered(channel))

        def onPacketChannelUnregistered(channel: PacketChannel, reason: Reason): Unit = dispatch(_.onPacketChannelUnregistered(channel, reason))

        def onPacketSent(packet: Packet, coordinates: PacketCoordinates): Unit = dispatch(_.onPacketSent(packet, coordinates))

        def onPacketReceived(packet: Packet, coordinates: PacketCoordinates): Unit = dispatch(_.onPacketReceived(packet, coordinates))

        def onPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit = dispatch(_.onPacketUsed(packet, coordinates))

        def onSystemOrderReceived(orderType: SystemOrder, reason: Reason): Unit = dispatch(_.onSystemOrderReceived(orderType, reason))

        def onSystemOrderSent(orderType: SystemOrder): Unit = dispatch(_.onSystemOrderSent(orderType))

        private def dispatch(f: EventListener => Unit): Unit = {
            listeners.foreach(f)
        }
    }
}
