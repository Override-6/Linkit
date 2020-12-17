package fr.`override`.linkit.api.exception

/**
 * thrown to report an unexpected packet
 *
 * @see [[Packet]]
 * */
class UnexpectedPacketException(message: String) extends PacketException(message)