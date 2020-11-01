package fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.util

import javafx.application.Platform

object FxPlatform {

    def runOnThread(action: Unit => Unit): Unit = {
        Platform.runLater(() => {
            action(())
            synchronized {
                notifyAll()
            }
        })
        synchronized {
            wait()
        }
    }

}
