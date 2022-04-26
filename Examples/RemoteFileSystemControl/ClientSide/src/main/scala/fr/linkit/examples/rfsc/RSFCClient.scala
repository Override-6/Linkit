package fr.linkit.examples.rfsc

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.client.config.schematic.ScalaClientAppSchematic

import java.net.InetSocketAddress
import java.nio.file.{CopyOption, Files, Path}

object RSFCClient {

    def main(args: Array[String]): Unit = {
        val serverStatics = launchApp("x").network.getStaticAccess(1)
        val path: Path    = serverStatics[Path].of[Path]("D:\\Users\\maxim\\Desktop\\Dev\\Perso\\Linkit\\StaticsFTPTest\\Server\\testServer.txt", Array[String]())
        val destPath      = Path.of("D:\\Users\\maxim\\Desktop\\Dev\\Perso\\Linkit\\StaticsFTPTest\\Client\\destination.png")
        serverStatics[Files].move(path, destPath, Array[CopyOption]())
    }


    private def launchApp(identifier0: String): ExternalConnection = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier   : String            = identifier0
                    override val remoteAddress: InetSocketAddress = new InetSocketAddress("localhost", 48489)
                }
            }
        }
        ClientApplication.launch(config, getClass)
            .findConnection(identifier0)
            .get
    }


}
