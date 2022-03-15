package fr.linkit.server.test.sync

import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.internal.mapping.ClassMappings
import fr.linkit.engine.test.Player
import fr.linkit.server.test.ServerLauncher
import org.junit.Test

class SyncObjectRMIPerformanceTests {

    @Test
    def testRMI(): Unit = {
        val app = ServerLauncher.launch()
        ClassMappings.putClass(classOf[Player])
        val connection = app.findConnection("TestServer1").get
        /*val behavior = new ContractDescriptorDataBuilder {
            describe(new ClassDescriptor[Player]() {
                enable method "add" withRule(BasicInvocationRule.BROADCAST)
            })
        }.build()*/
        val array = connection.network
            .globalCache
            .attachToCache(52, DefaultSynchronizedObjectCache[Player]())
            .syncObject(0, Constructor[Player](9, "test", "test", 9, 8))
        println(s"array = $array")
        Thread.sleep(4567890987654L)
    }

}
