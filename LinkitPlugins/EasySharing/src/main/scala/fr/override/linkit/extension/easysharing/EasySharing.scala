package fr.`override`.linkit.extension.easysharing

class EasySharing(relay: Relay) extends RelayExtension(relay) {

    private val remoteClipboard = new RemoteClipboard()
    private val remoteScreen = new RemoteScreen(relay.network)

    override def onLoad(): Unit = {
        putFragment(remoteClipboard)
        putFragment(remoteScreen)
    }

    override def onEnable(): Unit = {
        val commandManager = getFragmentOrAbort(classOf[ControllerExtension], classOf[CommandManager])
        commandManager.register("paste", new RemotePasteCommand(relay))
        commandManager.register("spectate", new RemoteScreenCommand(remoteScreen))
    }

}
