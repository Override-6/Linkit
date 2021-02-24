package fr.`override`.linkit.api.exception

case class RelayInitialisationException(msg: String, cause: Throwable = null) extends RelayException(msg, cause)