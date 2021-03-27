package fr.`override`.linkit.extension.easysharing.clipboard

import javax.imageio.ImageIO

class RemotePasteCommand(relay: Relay) extends CommandExecutor {

    private val network = relay.network
    private val fs = relay.configuration.fsAdapter

    override def execute(implicit args: Array[String]): Unit = {
        checkArgs(args)

        val target = args(0)
        val kind = args(1).toLowerCase
        val clipboardController = getClipboard(target)

        val action: (Array[String], RemoteClipboardController) => Unit = kind match {
            case "text" => pasteText
            case "img" | "image" => pasteImage
            case "paths" => pastePaths
        }
        action(args, clipboardController)
    }

    private def pasteImage(args: Array[String], clipboardController: RemoteClipboardController): Unit = {
        val path = args(2).toLowerCase
        if (path == "current") {
            clipboardController.pasteCurrentImage()
            println("Pasted current copied image !")
        } else {
            val image = ImageIO.read(fs.getAdapter(path).newInputStream())
            clipboardController.pasteImage(image)
            println(s"Pasted image at path $path")
        }
    }

    private def pasteText(args: Array[String], clipboardController: RemoteClipboardController): Unit = {
        if (args.length == 3 && args(2) == "current") {
            clipboardController.pasteCurrentText()
            println("Pasted current copied text !")
        } else {
            val text = args.drop(2).mkString(" ")
            clipboardController.paste(text)
            println(s"Pasted '$text' !")
        }
    }

    private def pastePaths(args: Array[String], clipboardController: RemoteClipboardController): Unit = {
        if (args.length == 3 && args(2) == "current") {
            clipboardController.pasteCurrentFiles()
            println("Pasted current copied paths !")
        } else {
            val paths = args.drop(2)
            clipboardController.pasteFiles(args.drop(2): _*)
            println(s"Pasted paths: ${paths.mkString("Array(", ", ", ")")} !")
        }
    }

    private def checkArgs(args: Array[String]): Unit = {
        if (args.length < 2)
            throw CommandException("Syntax: paste <target> <text|img|paths> <args...>")
    }

    private def getClipboard(target: String): RemoteClipboardController = {
        val entityOpt = network.getEntity(target)
        if (entityOpt.isEmpty)
            throw CommandException(s"'$target' isn't connected on the network.")

        val entity = entityOpt.get
        val controllerOpt = entity.getFragmentController("RemoteClipboard")
        if (controllerOpt.isEmpty)
            throw CommandException(s"RemoteClipboard fragment for $target is unavailable.")

        new RemoteClipboardController(controllerOpt.get)
    }

}
