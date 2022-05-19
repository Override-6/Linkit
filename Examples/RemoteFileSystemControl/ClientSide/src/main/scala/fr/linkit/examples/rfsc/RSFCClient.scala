package fr.linkit.examples.rfsc

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}

import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

object RSFCClient {

    def main(args: Array[String]): Unit = {
        val network       = launchApp("x").network
        val serverStatics = network.getStaticAccess(1)

        val srcPath = Path.of("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\StaticsFTPTests\\Client\\test.txt")
        if (Files.notExists(srcPath)) {
            Files.createDirectories(srcPath.getParent)
            Files.createFile(srcPath)
        }
        var pomme: Path = serverStatics[Path].of("pomme.txt", Array[String]())
        if (serverStatics[Files].exists(pomme)) { //si pomme existe déja
            pomme = serverStatics[Path].of("pomme (1).txt")
        }
        //on upload le contenu de srcPath dans pomme.txt
        val content                    = Files.readAllBytes(srcPath)
        val serverStream: OutputStream = serverStatics[Files].newOutputStream(pomme)
        serverStream.write(content)
        serverStream.close()
        serverStatics[System].exit(0)
        print("Done.")
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
