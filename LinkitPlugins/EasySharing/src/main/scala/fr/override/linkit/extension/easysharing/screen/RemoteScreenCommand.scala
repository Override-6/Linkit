package fr.`override`.linkit.extension.easysharing.screen

class RemoteScreenCommand(remoteScreen: RemoteScreen) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)
        val target = args(0)

        if (remoteScreen.isSpectating(target)) {
            remoteScreen.stopSpectating(target)
            println(s"Stopped spectating '$target' !")
            return
        }
        println(s"Spectating '$target'...")
        remoteScreen.spectate(target)
    }

    def checkArgs(args: Array[String]): Unit = {
        if (args.length != 1)
            throw CommandException("syntax: spectate <target>")
    }
}
