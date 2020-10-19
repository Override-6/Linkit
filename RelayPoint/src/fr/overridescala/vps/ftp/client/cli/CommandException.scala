package fr.overridescala.vps.ftp.client.cli

case class CommandException(msg: String) extends Exception(msg)
