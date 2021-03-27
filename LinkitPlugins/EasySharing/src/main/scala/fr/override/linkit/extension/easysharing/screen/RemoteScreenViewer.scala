package fr.`override`.linkit.extension.easysharing.screen

import java.io.ByteArrayInputStream

class RemoteScreenViewer extends Canvas {

    private val graphics = getGraphicsContext2D

    def pushFrame(bytes: Array[Int]): Unit = {
        val img = new Image(new ByteArrayInputStream(convertByteArray(bytes)))
        graphics.drawImage(img, 0, 0)
        println("Set !")
    }

}
