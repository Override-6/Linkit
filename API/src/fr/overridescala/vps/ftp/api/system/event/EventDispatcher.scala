package fr.overridescala.vps.ftp.api.system.event

import fr.overridescala.vps.ftp.api.`extension`.RelayExtension
import fr.overridescala.vps.ftp.api.system.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannelManager}
import fr.overridescala.vps.ftp.api.system.{Reason, SystemOrder}
import fr.overridescala.vps.ftp.api.task.Task

import scala.collection.mutable.ListBuffer

class EventDispatcher {

    val notifier = new EventNotifier

    def register(listener: EventListener): Unit = notifier.listeners += listener

}

object EventDispatcher {
    class EventNotifier {
        private[EventDispatcher] val listeners = ListBuffer.empty[EventListener]

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

        def onPacketChannelRegistered(channel: PacketChannelManager): Unit = dispatch(_.onPacketChannelRegistered(channel))

        def onPacketChannelUnregistered(channel: PacketChannelManager, reason: Reason): Unit = dispatch(_.onPacketChannelUnregistered(channel, reason))

        def onPacketSent(packet: Packet): Unit = dispatch(_.onPacketSent(packet))

        def onPacketReceived(packet: Packet): Unit = dispatch(_.onPacketReceived(packet))

        def onPacketUsed(packet: Packet): Unit = dispatch(_.onPacketUsed(packet))

        def onSystemOrderReceived(orderType: SystemOrder, reason: Reason): Unit = dispatch(_.onSystemOrderReceived(orderType, reason))

        def onSystemOrderSent(orderType: SystemOrder): Unit = dispatch(_.onSystemOrderSent(orderType))

        private def dispatch(f: EventListener => Unit): Unit = listeners.foreach(f)
    }
}
