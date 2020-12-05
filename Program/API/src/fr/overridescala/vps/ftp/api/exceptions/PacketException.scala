package fr.overridescala.vps.ftp.api.exceptions

case class PacketException(msg: String) extends RelayException(msg)