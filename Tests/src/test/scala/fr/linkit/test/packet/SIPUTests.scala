package fr.linkit.test.packet

import fr.linkit.api.gnom.packet.{DedicatedPacketCoordinates, PacketBundle}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.pool.{WorkerPool, WorkerPools}
import fr.linkit.engine.gnom.packet.traffic.PacketInjectionException
import fr.linkit.engine.gnom.packet.traffic.unit.SequentialInjectionProcessorUnit
import fr.linkit.mock.{AbstractPacketChannelMock, PacketDownloadAbstractMock}
import fr.linkit.test.packet.SIPUTests._
import org.junit.jupiter.api.{Assertions, Test}

import java.util.concurrent.locks.LockSupport
import scala.util.Random

class SIPUTests {

    private var injectedOrdinal: Int = 1
    private val mainThread           = Thread.currentThread()
    private val InjectionSize        = 2000

    @Test
    def injectSameOrdinal(): Unit = {
        Assertions.assertThrows(classOf[PacketInjectionException], () => runTest(pool10) {
            val sipu = newSIPU(ensureOrderedInjection)
            sipu.post(simulPacket(0))
            sipu.post(simulPacket(0))
        })
    }

    @Test
    def injectAsyncReverse(): Unit = {
        injectAsyncReverse(pool1)
        injectAsyncReverse(pool10)
        injectAsyncReverse(pool100)
        injectAsyncReverse(pool1000)
    }

    @Test
    def injectAsync(): Unit = {
        injectAsync(pool1)
        injectAsync(pool10)
        injectAsync(pool100)
        injectAsync(pool1000)
    }

    @Test
    def injectAsyncShuffled(): Unit = {
        injectAsyncShuffled(pool1)
        injectAsyncShuffled(pool10)
        injectAsyncShuffled(pool100)
        injectAsyncShuffled(pool1000)
    }


    def injectAsyncReverse(pool: WorkerPool): Unit = runTest(pool) {
        val sipu = newSIPU(ensureOrderedInjection)

        for (i <- InjectionSize to 1 by -1) pool.runLater {
            sipu.post(simulPacket(i))
        }
        LockSupport.park()
    }

    def injectAsync(pool: WorkerPool): Unit = runTest(pool) {
        val sipu = newSIPU(ensureOrderedInjection)

        for (i <- 1 to InjectionSize) pool.runLater {
            sipu.post(simulPacket(i))
        }
        LockSupport.park()
    }

    def injectAsyncShuffled(pool: WorkerPool): Unit = runTest(pool) {
        val sipu = newSIPU(ensureOrderedInjection)
        val list = Random.shuffle((1 to InjectionSize).toList)
        for (i <- list) pool.runLater {
            sipu.post(simulPacket(i))
        }
        LockSupport.park()
    }

    def runTest(pool: WorkerPool)(f: => Unit): Unit = {
        pool.runLaterControl(f).throwNextThrowable()
    }

    private def ensureOrderedInjection(bundle: PacketBundle): Unit = {
        val ord = bundle.ordinal.get
        println(s"ord = $ord")
        Assertions.assertEquals(injectedOrdinal, ord)
        injectedOrdinal += 1
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
    private val coords   = DedicatedPacketCoordinates(Array(1, 2, 3), "server", "client")
    private val pool1    = WorkerPools.newClosedPool("SIPUTests1", 5)
    private val pool10   = WorkerPools.newClosedPool("SIPUTests10", 10)
    private val pool100   = WorkerPools.newClosedPool("SIPUTests10", 100)
    private val pool1000 = WorkerPools.newClosedPool("SIPUTests100", 1000)
}
