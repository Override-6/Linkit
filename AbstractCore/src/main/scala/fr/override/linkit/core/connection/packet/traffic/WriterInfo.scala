package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.internal.system.event.packet.PacketEventHooks

case class WriterInfo(traffic: PacketTraffic,
                      identifier: Int,
                      transform: Packet => Packet,
                      notifier: EventNotifier,
                      packetHooks: PacketEventHooks)
