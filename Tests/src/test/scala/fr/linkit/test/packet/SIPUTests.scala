package fr.linkit.test.packet

import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, PacketBundle}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.PacketInjectionException
import fr.linkit.engine.gnom.packet.traffic.unit.SequentialInjectionProcessorUnit
import fr.linkit.mock.{AbstractPacketChannelMock, PacketDownloadAbstractMock}
import fr.linkit.test.packet.SIPUTests._
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.{Assertions, Test}
import org.opentest4j.AssertionFailedError

import java.util.concurrent.locks.LockSupport
import scala.util.Random

class SIPUTests {

    private var injectedOrdinal: Int = 1
    private val mainThread           = Thread.currentThread()
    private val InjectionSize        = 50000

    AppLoggers
    LogManager.shutdown()

    @Test
    def injectSameOrdinal(): Unit = {
        Assertions.assertThrows(classOf[PacketInjectionException], () => {
            val sipu = newSIPU(ensureOrderedInjection)
            sipu.post(simulPacket(0))
            sipu.post(simulPacket(0))
        })
    }

    @Test
    def injectAsyncReverse(): Unit = {
        val sipu = newSIPU(ensureOrderedInjection)

        var ex: Throwable = null
        for (i <- InjectionSize to 1 by -1) {
            val x = i
            pool.runLater {
                try {
                    sipu.post(simulPacket(x))
                } catch {
                    case e =>
                        ex = e
                        LockSupport.unpark(mainThread)
                }
            }
        }
        LockSupport.park()
        if (ex != null)
            throw ex
    }


    @Test
    def injectAsync(): Unit = {
        val sipu = newSIPU(ensureOrderedInjection)
        var ex: Throwable = null
        for (i <- 1 to InjectionSize) pool.runLater {
            try {
                val x = i
                if ((x % (InjectionSize / 20)) == 0)
                    println("sent: " + x)
                sipu.post(simulPacket(x))
            } catch {
                case e =>
                    ex = e
                    LockSupport.unpark(mainThread)
            }
        }
        LockSupport.park()
        if (ex != null)
            throw ex
    }

    @Test
    def injectAsyncChaos(): Unit = {
        val sipu = newSIPU(ensureOrderedInjection)
        var ex: Throwable = null
        for (i <- 1 to InjectionSize) pool.runLater {
            Thread.sleep(0L, (System.nanoTime() % 5000).toInt)
            try {
                val x = i
                if ((x % (InjectionSize / 20)) == 0)
                    println("sent: " + x)
                sipu.post(simulPacket(x))
            } catch {
                case e =>
                    ex = e
                    LockSupport.unpark(mainThread)
            }
        }
        LockSupport.park()
        if (ex != null)
            throw ex
    }

    private def dumpVThreads(): Unit = {
        println("DUMPING STACK TRACE")
        println()
        println()
        Thread.getAllStackTraces.keySet().toArray(new Array[Thread](_))
                .groupBy(_.getStackTrace.toList).foreach { case (st, threads) =>
            if (threads.length > 1)
                println(s"(${threads.length}) - ${threads.mkString(", ")}:")
            else println(threads.head.getName + ":")
            st.foreach(ste => println("\t" + ste))
        }
    }

    @Test
    def injectAsyncShuffled(): Unit = {
        val sipu = newSIPU(ensureOrderedInjection)
        val list = Random.shuffle((1 to InjectionSize).toList)
        var ex: Throwable = null
        for (i <- list) {
            val x = i
            pool.runLater {
                try {
                    sipu.post(simulPacket(x))
                } catch {
                    case e =>
                        ex = e
                        LockSupport.unpark(mainThread)
                }
            }
        }
        LockSupport.park()
        if (ex != null)
            throw ex
    }

    private def ensureOrderedInjection(bundle: PacketBundle): Unit = {
        val ord = bundle.ordinal.get
        //println(s"ord = $ord")
        if (injectedOrdinal != ord)
            throw new AssertionFailedError(s"expected: <$injectedOrdinal> but was <$ord>")
        Assertions.assertEquals(injectedOrdinal, ord)
        injectedOrdinal += 1
        if (injectedOrdinal % (InjectionSize / 20) == 0)
            println(s"received: $injectedOrdinal")
        if (injectedOrdinal == InjectionSize)
            LockSupport.unpark(mainThread)
    }

    private def simulPacket(ordinal: Int, _onDeserialize: => Unit = () => ()): PacketDownload = {
        new PacketDownloadAbstractMock(ordinal, coords) {
            override def onDeserialize(): Unit = _onDeserialize
        }
    }

    private def newSIPU(onInject: PacketBundle => Unit): SequentialInjectionProcessorUnit = {
        val channelMock = new AbstractPacketChannelMock {
            override def inject(bundle: PacketBundle): Unit = onInject(bundle)
        }
        new SequentialInjectionProcessorUnit(channelMock)
    }

}

object SIPUTests {
    private val coords = DedicatedPacketCoordinates(Array(1, 2, 3), NameTag("server"), NameTag("client"))
    private val pool   = Procrastinator("SIPUTests")
}
