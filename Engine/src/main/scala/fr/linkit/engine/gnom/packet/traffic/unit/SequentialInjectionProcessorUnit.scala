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

import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.Worker
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.{SimplePacketBundle, UnexpectedPacketException}
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController
import fr.linkit.engine.internal.debug.{Debugger, SIPURectifyAction}

import java.io.PrintStream
import java.lang.Thread.State._
import java.util.concurrent.locks.LockSupport
import java.util.{Comparator, PriorityQueue}
import scala.annotation.switch
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SequentialInjectionProcessorUnit(private val injectable: PacketInjectable) extends InjectionProcessorUnit {

    private final val queues           = mutable.HashMap.empty[String, OrdinalQueue]
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]
    private final val joiningThreads   = ListBuffer.empty[Thread]
    private final var executor: Worker = _

    private final val joinLocker       = new SimpleWorkerController()
    private final val refocusingLocker = new SIPUWorkerController()
    private       val reference        = injectable.reference

    //if refocusShift is > 0, this means that the executor is waiting for newer packets to come.
    //the value of refocusShift is equals to the difference between the expected packet ordinal and the ordinal of the most recent packet received.
    @volatile private final var refocusShift: Int = 0
    @volatile private final var packetCount                = 0

    /**
     * a short name for this type of unit
     * */
    override val shortname: String = "SIPU"

    override def post(result: PacketDownload): Unit = {
        AppLoggers.Traffic.trace(s"(SIPU $reference): adding packet (ord: ${result.ordinal}: ${result.coords.senderID}) Current unit executor: ${executor}.")


        queues.synchronized {
            val senderId = result.coords.senderID
            queues.getOrElseUpdate(senderId, new OrdinalQueue).offer(result)
            packetCount += 1
            if (refocusShift > 0) {
                refocusingLocker.wakeupAnyTask()
                refocusShift = 0
            }
        }
        val currentWorker = WorkerPools.currentWorker
        val currentTask   = currentWorker.getCurrentTask
        currentTask.synchronized {
            if (executor != null && (currentWorker ne executor)) {
                //the current thread is not the thread in charge of deserializing
                //and injecting packets for this InjectionProcessorUnit.
                if (executor.isSleeping) {
                    //If the thread that is in charge of the deserialization / injection process is doing nothing
                    //then force it to deserialize all remaining packets
                    AppLoggers.Traffic.info(s"(SIPU $reference): Turns out that this unit's executer is sleeping, waking up executor (${executor.thread.getName}) to inject remaining packets.")
                    try executor.runWhileSleeping(deserializeAll(true))
                    catch {
                        case _: IllegalThreadStateException =>
                    }
                    return
                }
                val threadState = executor.thread.getState
                (threadState: @switch) match {
                    case RUNNABLE | TIMED_WAITING =>
                        //thread is running (or is partially waiting),
                        // just add the packet and let the executor wakeup
                        return
                    case WAITING | BLOCKED        =>
                        //thread is blocked, let's transfer all the remaining deserialization / injection work to this thread.
                        //This operation has been made up in order to avoid possible deadlocks in local, or through the network.
                        //If the initial executor is notified or unparked, it will simply give up this SIPU.
                        AppLoggers.Traffic.info(s"(SIPU $reference): Turns out that this unit's executor is blocked. All injections of the executor (${executor.thread.getName}) are being transferred to this thread.")
                    //do not returns which will let the current thread enter in the deserialization process,
                    //when the initial executor will get unblocked, it'll detect that his work has been transferred to current thread and so will exit this unit.

                    case other =>
                        throw new IllegalThreadStateException(s"Could not inject packet into SIPU: unit's executor is in a dead state (${other.name().toLowerCase()}).")
                }
            }
            executor = currentWorker
        }

        deserializeAll(false)
    }

    def getCurrentOrdinal: Int = -1

    private def nextResult(): PacketDownload = {
        val queues   = this.queues.values
        var minQueue = queues.head
        if (queues.size > 1) {
            minQueue = null
            var minQueueUpdate = Long.MaxValue
            for (queue <- queues) if (queue.nonEmpty) {
                val lastUpdate = queue.lastUpdate
                if (lastUpdate < minQueueUpdate) {
                    minQueueUpdate = lastUpdate
                    minQueue = queue
                }
            }
        }
        packetCount -= 1
        minQueue.remove()
    }

    private def deserializeAll(duringSleep: Boolean): Unit = {
        val currentWorker = WorkerPools.currentWorker
        val currentTask   = currentWorker.getCurrentTask
        for (unit <- chain) {
            unit.join()
            if (executor ne currentWorker) {
                //work of the initial executor thread has been stolen by another thread (see #post)
                return //abort
            }
        }
        // Only the executor thread can run here
        //deserializing all packets that are added in the queue
        while (packetCount > 0) {
            if (executor ne currentWorker)
                return
            deserializeNextResult()
        }
        if (executor ne currentWorker)
            return
        //everything deserialized, now realising this deserialization unit.
        currentTask.synchronized {
            AppLoggers.Traffic.trace(s"Releasing current unit...")
            if (!duringSleep) executor = null
            joinLocker.wakeupAllTasks()
            joiningThreads.foreach(LockSupport.unpark)
            joiningThreads.clear()
        }
    }

    /**
     * Will make the current thread wait until this unit have terminated all his deserialisation / injection work.
     * */
    def join(): Unit = {
        if (executor == null) return //the current IPU is not processing any injection
        AppLoggers.Traffic.debug(s"(SIPU $reference): Waiting for unit to end injections before continuing")
        val currentThread = Thread.currentThread()
        val currentTask   = WorkerPools.currentTask.orNull
        if (currentTask == null && executor != null) {
            joiningThreads += currentThread
            LockSupport.park()
            joiningThreads -= currentThread
        } else if (executor != null) {
            AppLoggers.Traffic.trace(s"Pausing task, waiting for '${executor.thread.getName}' to finish.")
            currentTask.synchronized {}
            joiningThreads += currentThread
            joinLocker.pauseTask()
            joiningThreads -= currentThread
        }
    }

    private def deserializeNextResult(): Unit = {
        val result = nextResult()
        if (result == null) return
        AppLoggers.Traffic.trace(s"(SIPU $reference): handling packet deserialization and injection (ord: ${result.ordinal})")
        result.makeDeserialization()
        val bundle = SimplePacketBundle(result)
        injectable.inject(bundle)
    }

    def chainWith(unit: SequentialInjectionProcessorUnit): Unit = {
        chain += unit
    }

    private[traffic] def dump(out: PrintStream, indentLevel: Int): Unit = {
        val indentStr = " " * indentLevel

        out.print(indentStr)
        if (executor != null) out.println(s"- executor: ${executor.thread.getName} (${executor.thread.getState.name().toLowerCase})")
        else out.println("- no executor")

        if (chain.nonEmpty) {
            out.print(indentStr)
            out.println("- chained units: " + chain.map(x => x.injectable.trafficPath.mkString("/", "/", "")).mkString("", "; ", ";"))
        }
        if (joiningThreads.nonEmpty) {
            out.print(indentStr)
            out.println(s"- threads waiting this unit: ${joiningThreads.map(_.getName).mkString(", ")}")
        }
        val pendingPackets = queues.values.map(_.size).sum
        out.print(indentStr)
        if (pendingPackets == 0) {
            out.println("- this unit has no pending packets")
        } else {
            out.print(s"- this unit has $pendingPackets pending packets. ")
            if (refocusShift > 0) out.println(s"Those packets ordinals are $refocusShift ahead of expected ordinal.")
            else out.println()
        }
    }

    private class OrdinalQueue {

        private val queue          = new PriorityQueue[PacketDownload](((a, b) => a.ordinal - b.ordinal): Comparator[PacketDownload])
        private var currentOrdinal = 0

        var lastUpdate: Long = 0

        def nonEmpty: Boolean = queue.size() > 0

        def size: Int = queue.size()

        def offer(packet: PacketDownload): Unit = {
            queue.synchronized(queue.offer(packet))
            lastUpdate = System.currentTimeMillis()
        }

        def remove(): PacketDownload = {
            val result = queue.synchronized(queue.remove())
            if (result == null)
                throw new NullPointerException("queue.remove returned null.")
            val queueLength     = queue.size
            val resultOrd       = result.ordinal
            val expectedOrdinal = currentOrdinal + 1
            if (resultOrd != expectedOrdinal) {
                val diff = resultOrd - expectedOrdinal
                if (diff < 0) {
                    throw UnexpectedPacketException(s"in channel ${injectable.reference}: Received packet with ordinal '$resultOrd', while current ordinal is $expectedOrdinal : a packet has already been handled with ordinal number $resultOrd")
                }
                refocusShift = diff
                if (queueLength != queue.size) { //the queue was updated, maybe the remaining packets has been added
                    refocusShift = 0
                    queue.offer(result)
                    return nextResult()
                }
                if (refocusShift > 0) {
                    Debugger.push(SIPURectifyAction(reference, currentOrdinal, expectedOrdinal))
                    AppLoggers.Traffic.debug(s"(SIPU $reference): Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal. This unit will wait for the remaining $diff packets before handling other packets.")
                    refocusingLocker.pauseTask()
                    refocusShift = 0
                    Debugger.pop()
                }
                if (queue.peek().ordinal == resultOrd)
                    throw UnexpectedPacketException(s"in channel ${injectable.reference}: received packet with same ordinal after refocus.")
                queue.offer(result)
                return nextResult()
            }
            currentOrdinal = expectedOrdinal
            AppLoggers.Traffic.trace(s"(SIPU $reference): current ordinal is now $currentOrdinal.")
            result
        }

    }

    class SIPUWorkerController extends SimpleWorkerController {
        override protected def createControlTicket(pauseCondition: => Boolean): Unit = {
            if (!pauseCondition) return
            super.createControlTicket(pauseCondition)
        }
    }

}