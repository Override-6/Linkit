package fr.overridescala.vps.ftp.api.exceptions

import fr.overridescala.vps.ftp.api.packet.ext.fundamental.DataPacket

/**
 * thrown to report an unexpected packet
 *
 * @see [[DataPacket]]
 * */
case class UnexpectedPacketException(msg: String) extends RelayException(msg)
