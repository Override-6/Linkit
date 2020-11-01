package fr.overridescala.vps.ftp.extension.ppc.logic.fx

import fr.overridescala.vps.ftp.`extension`.ppc.logic.fx.player.LocalFxPlayer
import javafx.application.Application
import javafx.application.Application.launch
import javafx.stage.Stage

object MainTests {

    def main(args: Array[String]): Unit = {
        launch(classOf[App])
    }

    class App extends Application {
        override def start(primaryStage: Stage): Unit = {
            new GameInterface(new LocalFxPlayer("mami"), new LocalFxPlayer("papi")).startGame()
        }
    }

}
