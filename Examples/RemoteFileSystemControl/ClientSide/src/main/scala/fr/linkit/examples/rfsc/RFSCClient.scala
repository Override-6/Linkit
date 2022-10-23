package fr.linkit.examples.rfsc

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}

import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.util.concurrent.ThreadLocalRandom

object RFSCClient {

    def main(args: Array[String]): Unit = {
       // print("how many connection ? : ")
        /*val int = StdIn.readInt()
        if (int == 1) */launchAlone()
        //else launchXTimes(int)
    }

    private def launchXTimes(x: Int): Unit = {
        val connection = launchConnectionMultiple(1, x)
        Contract.registerProperties(ObjectsProperty.empty("fs"))
        connection.listConnections.foreach(cc => {
            remoteFS(cc.network)
        })
    }

    private def launchAlone(): Unit = {
        val identifier = "x" + ThreadLocalRandom.current().nextInt(10_000, 100_000)
        val network    = launchConnection(identifier).network
        Contract.registerProperties(ObjectsProperty.empty("fs"))
        remoteFS(network)
    }

    private def remoteFS(network: Network): Unit = {
        println(s"Connecting ${network.currentEngine.identifier}: ")
        val serverStatics = network.getStaticAccess(1)

        listenDistantDir(serverStatics)
        sendFile(serverStatics)

        println("Done.")
    }

    private def listenDistantDir(serverStatics: StaticAccess): Unit = {
        val dir: Path = serverStatics[Path].of("/")
        val serverfs  = serverStatics[FileSystems].getDefault(): FileSystem
        val watcher   = serverfs.newWatchService()
        dir.register(watcher, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE)
        new Thread(() => while (true) {
            val key = watcher.take()
            key.pollEvents().forEach {
                event => {
                    val path  : Path    = serverStatics[Path].of(event.context().toString)
                    val exists: Boolean = true /*serverStatics[Files].exists(path)*/
                    println(s"path $path exists: $exists")
                    event.kind() match {
                        case ENTRY_DELETE => println(s"Deleted file '$path'")
                        case ENTRY_CREATE => println(s"Created file '$path'")
                        case ENTRY_MODIFY => if (exists) {
                            val content = serverStatics[Files].readString(path): String
                            println(s"Modified file '$path'")
                            println(s"content is now:")
                            println(content)
                        }
                    }
                }
            }
            key.reset()

        }, "remote directory listener").start()
    }

    private def sendFile(serverStatics: StaticAccess): Unit = {
        val srcPath = Path.of("Examples/RemoteFileSystemControl/ClientSide/src/main/resources/pomme.txt")
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
    }

    private def launchConnectionMultiple(from: Int, to: Int): ApplicationContext = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "/home/maxime/dev/Linkit/Default_Home"
            logfilename = Some(s"examples/rfsc client from $from to $to")
            loadSchematic = new ScalaClientAppSchematic {
                for (i <- from to to) {
                    clients += new ClientConnectionConfigBuilder {
                        override val identifier: String = i.toString
                        override val remoteAddress      = new InetSocketAddress("localhost", 48489)
                        defaultPersistenceConfigScript = Some(getClass.getResource("/FS_persistence.sc"))
                    }
                }
            }
        }
        ClientApplication.launch(config, getClass)
    }

    private def launchConnection(identifier0: String): ExternalConnection = {
        println(s"Using identifier $identifier0")
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "/home/maxime/dev/Linkit/Default_Home"
            logfilename = Some(s"examples/rfsc client $identifier0")
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier: String = identifier0
                    override val remoteAddress      = new InetSocketAddress("localhost", 48489)
                    defaultPersistenceConfigScript = Some(getClass.getResource("/FS_persistence.sc"))
                }
            }
        }
        ClientApplication.launch(config, getClass)
            .findConnection(identifier0)
            .get
    }

}