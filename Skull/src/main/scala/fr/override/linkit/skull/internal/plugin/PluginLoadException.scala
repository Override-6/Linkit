package fr.`override`.linkit.skull.internal.plugin

import fr.`override`.linkit.skull.connection.task.TaskException


override.linkit.api.extension

case class PluginLoadException(msg: String, cause: Throwable = null) extends TaskException(msg, cause)
