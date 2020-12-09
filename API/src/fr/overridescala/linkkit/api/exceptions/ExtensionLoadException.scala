package fr.overridescala.linkkit.api.exceptions

case class ExtensionLoadException(msg: String, cause: Throwable = null) extends TaskException(msg, cause)