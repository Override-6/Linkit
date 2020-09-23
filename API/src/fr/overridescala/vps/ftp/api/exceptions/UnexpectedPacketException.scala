package fr.overridescala.vps.ftp.api.exceptions

/**
 * thrown to report an unexpected packet
 *
 * @see [[fr.overridescala.vps.ftp.api.packet.DataPacket]]
 * */
case class UnexpectedPacketException(msg: String) extends RelayException(msg)
