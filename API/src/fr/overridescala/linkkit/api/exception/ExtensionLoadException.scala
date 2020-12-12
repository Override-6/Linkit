package fr.overridescala.linkkit.api.exception

case class ExtensionLoadException(msg: String, cause: Throwable = null) extends TaskException(msg, cause)