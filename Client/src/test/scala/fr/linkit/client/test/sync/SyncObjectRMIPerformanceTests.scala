package fr.linkit.client.test.sync

import java.net.InetSocketAddress

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.internal.mapping.ClassMappings
import fr.linkit.engine.test.Player
import org.junit.jupiter.api.Test


class SyncObjectRMIPerformanceTests {

    @Test
    def rmiTests(): Unit = {
        val connection = launchApp("Testing")
        ClassMappings.putClass(classOf[Player])
        /*val behavior = new ContractDescriptorDataBuilder {
            describe(new ClassDescriptor[Player]() {
                enable method "add" withRule(BasicInvocationRule.BROADCAST)
            })
        }.build()*/
        val global = connection.network.globalCache
        val player = global
            .attachToCache(52, DefaultSynchronizedObjectCache[Player]())
            .findObject(0).get
        var i      = 0
        global.getCacheTrafficNode(52).setPerformantInjection()

        while (i < 1000) {
            i += 1
            val t0 = System.currentTimeMillis()
            player.name = "HEHE"
            val t1 = System.currentTimeMillis()
            println(s"Took ${t1 - t0} ms")
        }
        val player2 = Player(9, "test", "test", 9, 8)
        i = 0
        while (i < 1000) {
            i += 1
            val t0 = System.currentTimeMillis()
            player2.name = ("HEHE")
            val t1 = System.currentTimeMillis()
            println(s"Took ${t1 - t0} ms")
        }
        Thread.sleep(567898765)
    }


    private def launchApp(identifier0: String): ExternalConnection = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\Linkit\\Home"
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier   : String            = identifier0
                    override val remoteAddress: InetSocketAddress = new InetSocketAddress("localhost", 48484)
                }
            }
        }
        ClientApplication.launch(config, getClass)
            .findConnection(identifier0)
            .get
    }

}
