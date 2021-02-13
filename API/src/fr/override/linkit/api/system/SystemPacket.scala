package fr.`override`.linkit.api.system

import fr.`override`.linkit.api.packet.Packet

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet