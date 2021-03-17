package fr.`override`.linkit.core.internal.system

import fr.`override`.linkit.skull.connection.packet.Packet

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet