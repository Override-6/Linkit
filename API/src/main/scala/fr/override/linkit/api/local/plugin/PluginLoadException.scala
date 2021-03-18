package fr.`override`.linkit.api.local.plugin

import fr.`override`.linkit.api.connection.task.TaskException

case class PluginLoadException(msg: String, cause: Throwable = null) extends TaskException(msg, cause)
