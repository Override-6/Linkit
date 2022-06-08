package fr.linkit.examples.rfsc

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}

import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.util.concurrent.ThreadLocalRandom

object RSFCClient {
    
    def main(args: Array[String]): Unit = {
        val network       = launchApp("x" + ThreadLocalRandom.current().nextInt(10_000, 100_000)).network
        val serverStatics = network.getStaticAccess(1)
        
        listenDistantDir(serverStatics)
        sendFile(serverStatics)
        
        //serverStatics[System].exit(0)
        print("Done.")
    }
    
    
    private def listenDistantDir(serverStatics: StaticAccess): Unit = {
        val dir: Path = serverStatics[Path].of("/")
        val watcher   = (serverStatics[FileSystems].getDefault(): FileSystem).newWatchService()
        dir.register(watcher, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE)
        new Thread(() => while (true) {
            val key = watcher.take()
            key.pollEvents().forEach {
                event => println(s"${event.context()}: ${event.kind()}")
            }
            key.reset()
            
        }, "remote directory listener").start()
    }
    
    private def sendFile(serverStatics: StaticAccess): Unit = {
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
    }
    
    private def launchApp(identifier0: String): ExternalConnection = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier    = identifier0
                    override val remoteAddress = new InetSocketAddress("localhost", 48489)
                    defaultPersistenceConfigScript = Some(getClass.getResource("/FS_persistence.sc"))
                }
            }
        }
        ClientApplication.launch(config, getClass)
                .findConnection(identifier0)
                .get
    }
    
}
