package fr.`override`.linkit.core.local.system

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.local.system.CloseReason

case class SystemPacket private(order: SystemOrder,
                                reason: CloseReason,
                                content: Array[Byte] = Array()) extends Packet