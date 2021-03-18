package fr.`override`.linkit.core.local.system

import fr.`override`.linkit.api.connection.packet.Packet

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet