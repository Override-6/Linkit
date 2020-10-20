package fr.overridescala.vps.ftp.client.cli.commands

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.client.auto.{AutoUploader, AutomationManager}
import fr.overridescala.vps.ftp.client.cli.{CommandException, CommandExecutor, CommandUtils}

class ListenDirCommand(relay: Relay, automationManager: AutomationManager) extends CommandExecutor {


    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)
        val target = CommandUtils.argAfter("-t")
        val targetedFolder = CommandUtils.argAfter("-tf")
        val currentFolder = CommandUtils.argAfter("-cf")
        automationManager.register(new AutoUploader(relay, target, currentFolder, targetedFolder))
    }


    def checkArgs(implicit args: Array[String]): Unit = {
        if (args.length != 6)
            throw CommandException("usage : listen -t <target> -tf <target_folder> -cf <current_folder>")
        CommandUtils.checkArgsContains("-t", "-tf", "-cf")
    }

}
