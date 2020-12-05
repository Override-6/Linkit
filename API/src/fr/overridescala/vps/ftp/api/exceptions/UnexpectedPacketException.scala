package fr.overridescala.vps.ftp.api.exceptions

/**
 * thrown to report an unexpected packet
 *
 * @see [[Packet]]
 * */
class UnexpectedPacketException(message: String) extends PacketException(message)