/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.unit

import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.SimplePacketBundle
import fr.linkit.engine.gnom.packet.traffic.PacketInjectionException
import fr.linkit.engine.internal.debug.cli.SectionedPrinter
import fr.linkit.engine.internal.debug.{Debugger, SIPURectifyStep}

import java.util.{Comparator, PriorityQueue}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Sequential IPUs guarantees the order packets being treated for injection
 * */
class SequentialInjectionProcessorUnit(private val injectable: PacketInjectable) extends InjectionProcessorUnit {

    private final val queues           = mutable.HashMap.empty[NameTag, OrdinalQueue]
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]
    private final var executor: Thread = _

    private val reference = injectable.reference

    //if refocusShift is > 0, this means that the executor is waiting for newer packets to come.
    //the value of refocusShift is equals to the difference between the expected packet ordinal and the ordinal of the most recent packet received.
    private final var refocusShift: Int = 0

    private final val contentLock = new Object
    private final val accessLock  = new Object

    private final val joiningThreads = ListBuffer.empty[Thread] //for debugger traffic dump

    /**
     * a short name for this type of unit
     * */
    override val shortname: String = "SIPU"

    override def post(result: PacketDownload): Unit = {

        contentLock.synchronized {
            AppLoggers.Traffic.trace(s"(SIPU $reference): adding packet (ord: ${result.ordinal}: ${result.coords.senderTag}) Current unit executor: ${executor}.")
            val senderId = result.coords.senderTag
            queues.getOrElseUpdate(senderId, new OrdinalQueue).offer(result)
            if (refocusShift > 0) {
                if (executor eq null)
                    throw new IllegalStateException("refocusing without any executor.")
                refocusShift = 0
                contentLock.notifyAll() //will notify the executor
            }

            if (shouldInject())
                injectAll()
        }
    }

    /**
     * Determines if this unit should be handled by the executor thread.
     *
     * @return true if the current thread must handle the injection, false instead
     * */
    private def shouldInject(): Boolean = accessLock.synchronized {
        /*
        * This method will process as is:
        * 1. If no worker is currently processing this unit, or if the current worker is already this unit's executor, return true
        * */
        val currentWorker = Thread.currentThread()

        /* 1.
        * 1-1. the unit has no executor, the current worker will start the injection.
        * 1-2. the current worker is the executor of this unit.
        *      There are many reasons why an executor may end up injecting its own packets into its own unit.
        *      By returning true, the remaining packets will get injected.
        *     1.1                     1.2            */
        executor == null || (currentWorker eq executor)
    }

    private def pollNext(): PacketDownload = {
        while (true) {
            val minQueue = pollQueue()
            if (minQueue == null) return null //there are no such elements, return null
            val result = minQueue.remove()
            if (result ne null) return result
        }
        null
    }

    private def pollQueue(): OrdinalQueue = contentLock.synchronized {
        val values   = this.queues.values
        var minQueue = values.head
        val queues   = values.iterator
        if (values.size > 1) {
            minQueue = null
            var minQueueUpdate = Long.MaxValue
            while (queues.hasNext) {
                val queue = queues.next()
                if (queue.nonEmpty) {
                    val lastUpdate = queue.lastUpdate
                    if (lastUpdate < minQueueUpdate) {
                        minQueueUpdate = lastUpdate
                        minQueue = queue
                    }
                }
            }
        }
        if (minQueue != null && minQueue.size == 0)
            return null //if queue is empty there is no queue ready
        minQueue
    }

    private def injectAll(): Unit = {
        val currentWorker = Thread.currentThread()
        executor = currentWorker

        // Only the executor thread can run here

        for (unit <- chain)
            unit.join()
        //deserializing all packets that are added in the queue
        var next = pollNext()
        while (next != null) {
            deserializeAndInject(next)
            next = pollNext()
        }
        accessLock.synchronized {
            //everything deserialized, now realising this unit.
            executor = null
            AppLoggers.Traffic.trace(s"Releasing current unit ${injectable.trafficPath.mkString("/", "/", "")}...")
            accessLock.notifyAll()
        }
    }

    /**
     * Will make the current thread wait until this unit have terminated all his deserialisation / injection work.
     * */
    def join(): Unit = accessLock.synchronized {
        if (executor == null) return //the current IPU is not processing any injection
        AppLoggers.Traffic.debug(s"(SIPU $reference): Waiting for unit to end injections before continuing")
        val currentThread = Thread.currentThread()
        if (currentThread != null || executor == null) {
            joiningThreads += currentThread
            accessLock.wait()
            joiningThreads -= currentThread
        }
    }

    private def deserializeAndInject(result: PacketDownload): Unit = {
        AppLoggers.Traffic.trace(s"(SIPU $reference): handling packet deserialization and injection (ord: ${result.ordinal})")
        result.makeDeserialization()
        AppLoggers.Traffic.trace(s"(SIPU $reference): now injecting...")
        val bundle = SimplePacketBundle(result)
        injectable.inject(bundle)
        AppLoggers.Traffic.trace("(SIPU $reference): inject done")
    }

    def chainWith(unit: SequentialInjectionProcessorUnit): Unit = {
        chain += unit
    }

    private[traffic] def appendDump(printer: SectionedPrinter)(sec: printer.Section, indentLevel: Int): Unit = {
        import printer._
        val indentStr = " " * indentLevel
        sec.append(indentStr).append('\n')
        if (executor != null) {
            sec.append(s"- executor: ${executor.getName} (${executor.getState.name().toLowerCase})")
                    .enable()
        } else sec.append("- no executor\n")

        if (chain.nonEmpty) {
            sec.append(indentStr)
                    .append("- chained units: " + chain.map(x => x.injectable.trafficPath.mkString("/", "/", "")).mkString("", "; ", ";"))
                    .append('\n')
        }
        if (joiningThreads.nonEmpty) {
            sec.append(indentStr)
                    .append(s"- threads waiting this unit: ${joiningThreads.map(_.getName).mkString(", ")}\n")
                    .enable()
        }
        val pendingPackets = queues.values.map(_.size).sum
        sec.append(indentStr)
        if (pendingPackets == 0) {
            sec.append("- this unit has no pending packets\n")
        } else {
            sec.enable()
                    .append(s"- this unit has $pendingPackets pending packets. ")
            if (refocusShift > 0) sec.append(s"Those packets ordinals are $refocusShift ahead of expected ordinal.\n")
            else sec.append('\n')
        }
    }

    private class OrdinalQueue {

        private val queue          = new PriorityQueue[PacketDownload](((a, b) => a.ordinal - b.ordinal): Comparator[PacketDownload])
        private var currentOrdinal = 0

        var lastUpdate: Long = 0

        def nonEmpty: Boolean = queue.size() > 0

        def size: Int = queue.size()

        def offer(packet: PacketDownload): Unit = queue.synchronized {
            queue.offer(packet)
            lastUpdate = System.currentTimeMillis()
        }

        def remove(): PacketDownload = {
            val result = queue.synchronized(queue.remove())

            val resultOrd       = result.ordinal
            val expectedOrdinal = currentOrdinal + 1
            if (resultOrd != expectedOrdinal) {
                val diff = resultOrd - expectedOrdinal
                if (diff < 0) {
                    throw new PacketInjectionException(s"in channel ${injectable.reference}: Received packet with ordinal $resultOrd, but expected was $expectedOrdinal : a packet has already been handled with ordinal number $resultOrd")
                }
                contentLock.synchronized {
                    Debugger.push(SIPURectifyStep(reference, resultOrd, expectedOrdinal))
                    AppLoggers.Traffic.debug(s"(SIPU $reference): Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal. This unit will wait for the remaining $diff packets before handling other packets.")
                    refocusShift = diff
                    contentLock.wait()
                    refocusShift = 0
                    Debugger.pop()
                }
                queue.synchronized(queue.offer(result))
                return null
            }
            currentOrdinal = expectedOrdinal
            AppLoggers.Traffic.trace(s"(SIPU $reference): current ordinal is now $currentOrdinal.")
            result
        }

    }


}
