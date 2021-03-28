package fr.linkit.plugin.controller

import fr.linkit.api.local.plugin.LinkitPlugin
import fr.linkit.plugin.controller.cli.CommandManager
import fr.linkit.plugin.controller.cli.commands.ShutdownCommand

class ControllerExtension extends LinkitPlugin {

    private val commandManager = new CommandManager()

    override def onLoad(): Unit = {
        putFragment(commandManager)
    }

    override def onEnable(): Unit = {
        commandManager.register("stop", new ShutdownCommand(getContext))
        commandManager.start()
    }
}