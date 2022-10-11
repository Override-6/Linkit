package fr.linkit.test.packet

import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, PacketBundle}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.packet.traffic.PacketInjectionException
import fr.linkit.engine.gnom.packet.traffic.unit.SequentialInjectionProcessorUnit
import fr.linkit.mock.{AbstractPacketChannelMock, PacketDownloadAbstractMock}
import fr.linkit.test.packet.SIPUTests._
import org.junit.jupiter.api.{Assertions, Test}
import org.opentest4j.AssertionFailedError

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable.ListBuffer
import scala.io.StdIn
import scala.util.Random

class SIPUTests {

    private var injectedOrdinal: Int = 1
    private val mainThread           = Thread.currentThread()
    private val InjectionSize        = 1_000_000

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

        for (i <- InjectionSize to 1 by -1) {
            val x = i
            pool.runLater {
                sipu.post(simulPacket(x))
            }
        }
        LockSupport.park()
    }


    @Test
    def injectAsync(): Unit = {
        val sipu = newSIPU(ensureOrderedInjection)

        for (i <- 1 to InjectionSize) pool.runLater {
            val x = i
            if ((x % (InjectionSize / 20)) == 0)
                println("sent: " + x)
            sipu.post(simulPacket(x))
        }
        dumpVThreads()
        LockSupport.park()
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
        for (i <- list) {
            val x = i
            pool.runLater {
                sipu.post(simulPacket(x))
            }
        }
        LockSupport.park()
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
    private val coords = DedicatedPacketCoordinates(Array(1, 2, 3), "server", "client")
    private val pool   = Procrastinator("SIPUTests")
}
