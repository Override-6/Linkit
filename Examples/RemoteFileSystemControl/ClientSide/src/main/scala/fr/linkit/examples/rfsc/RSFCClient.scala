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
        val network               = launchApp("x").network
        val serverStatics         = network.getStaticAccess(1)

        val srcPath               = Path.of("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\StaticsFTPTests\\Client\\test.txt")
        if (Files.notExists(srcPath)) {
            Files.createDirectories(srcPath.getParent)
            Files.createFile(srcPath)
        }
        val content = Files.readAllBytes(srcPath)
        val destPath: Path        = serverStatics[Path].of[Path]("test.txt", Array[String]())
        val serverStream: OutputStream = serverStatics[Files].newOutputStream[OutputStream](destPath)
        serverStream.write(content)
        serverStream.close()

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
