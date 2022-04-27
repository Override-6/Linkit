package fr.linkit.examples.rfsc

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

object RSFCClient {

    implicit def cast[A, B, X[C, D]](x: X[_, _]): X[A, B] = x.asInstanceOf[X[A, B]]

    val a: String = "dsdq"
    val b: Function1[Int, Int] = null

    val c: Function1[String, String] = b

    def main(args: Array[String]): Unit = {
        val network = launchApp("x").network
        val serverStatics = network.getStaticAccess(1)
        val path: Path    = serverStatics[Path].of[Path]("testServer.txt", Array[String]())
        val destPath      = Path.of("D:\\Users\\maxim\\Desktop\\Dev\\Perso\\Linkit\\StaticsFTPTest\\Client\\destination.png")
        val content: Array[Byte] = serverStatics[Files].readAllBytes[Array[Byte]](path)
        if (!Files.exists(destPath)) {
            Files.createDirectories(destPath.getParent)
            Files.createFile(destPath)
        }
        Files.write(destPath, content)
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
