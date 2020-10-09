package fr.overridescala.vps.ftp.api.exceptions

/**
 * thrown to report an unexpected packet
 *
 * @see [[Packet]]
 * */
case class UnexpectedPacketException(msg: String) extends RelayException(msg)
