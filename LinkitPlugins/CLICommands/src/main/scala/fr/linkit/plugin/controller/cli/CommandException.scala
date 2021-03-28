package fr.linkit.plugin.controller.cli

case class CommandException(msg: String) extends Exception(msg)
