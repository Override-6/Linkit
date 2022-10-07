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
import fr.linkit.engine.internal.concurrency.pool.SimpleTaskController
import fr.linkit.engine.internal.debug.{Debugger, SIPURectifyStep}

import java.io.PrintStream
import java.lang.Thread.State._
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.{LockSupport, ReentrantLock}
import java.util.{Comparator, PriorityQueue}
import scala.annotation.switch
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Sequential IPUs guarantees the order packets being treated for injection
 * */
class SequentialInjectionProcessorUnit(private val injectable: PacketInjectable) extends InjectionProcessorUnit {

    private final val queues           = mutable.HashMap.empty[String, OrdinalQueue]
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]
    private final val joiningThreads   = ListBuffer.empty[Thread]
    private final var executor: Worker = _

    private final val refocusingLocker = new SIPUTaskController()
    private       val reference        = injectable.reference

    //if refocusShift is > 0, this means that the executor is waiting for newer packets to come.
    //the value of refocusShift is equals to the difference between the expected packet ordinal and the ordinal of the most recent packet received.
    private final var refocusShift: Int = 0

    private final val unitContentLock    = new Object
    private final val accessLock         = new ReentrantLock()
    private final val joinTaskController = new SimpleTaskController()


    /**
     * a short name for this type of unit
     * */
    override val shortname: String = "SIPU"

    override def post(result: PacketDownload): Unit = {
        AppLoggers.Traffic.trace(s"(SIPU $reference): adding packet (ord: ${result.ordinal}: ${result.coords.senderID}) Current unit executor: ${executor}.")

        unitContentLock.synchronized {
            val senderId = result.coords.senderID
            queues.getOrElseUpdate(senderId, new OrdinalQueue).offer(result)
            if (refocusShift > 0) {
                refocusingLocker.wakeupAnyTask()
                refocusShift = 0
            }
        }

        if (shouldInject())
            injectAll(false)
    }

    /**
     * Determines if this unit should be handled by the executor thread.
     *
     * @return true if the current thread must handle the injection, false instead
     * */
    private def shouldInject(): Boolean = try {
        /*
        * This method will process as is:
        * 1. If no worker is currently processing this unit, or if the current worker is already this unit's executor, return true
        * 2. If the current executor is sleeping, wake it up to continue injection.
        * 3. Check the executor state (its thread state) and determine if the current worker should replace it or not.
        * */
        accessLock.lock()
        val currentWorker = WorkerPools.currentWorker

        /* 1.
        * 1-1. the unit has no executor, the current worker will start the injection.
        * 1-2. the current worker is the executor of this unit.
        *      There are many reasons why an executor may end up injecting its own packets into its own unit.
        *      By returning true, the remaining packets will get injected.
        *          1.1                     1.2            */
        if (executor == null || (currentWorker eq executor))
            return true

        /* 2.
        * If the executor is doing nothing (is sleeping)
        * then force it to wakeup and inject remaining packets
        * */
        if (executor.isSleeping) {
            AppLoggers.Traffic.info(s"(SIPU $reference): Turns out that this unit's executer is sleeping, waking up executor (${executor.thread.getName}) to inject remaining packets.")
            try executor.wakeupAndRun(injectAll(true))
            catch {
                case _: IllegalThreadStateException =>
            }
            return false
        }

        /* 3.
        * 3-1. If the executor thread state is RUNNABLE (executing) or TIMED_WAITING (temporarily waiting), the current worker will
        * choose to let this unit's executor run.
        * 3-2. If the executor thread state is WAITING or BLOCKED, the current worker will choose to replace it.
        * 3-3. If the executor thread state is NEW or TERMINATED, throw an exception
        * */
        val threadState = executor.thread.getState
        (threadState: @switch) match {
            case RUNNABLE | TIMED_WAITING =>
                //thread is running (or is partially waiting), just let the current worker wakeup or finish
                false
            case WAITING | BLOCKED        =>
                /*
                 * thread is blocked, let's transfer all the remaining injection work to this thread.
                 * This operation has been made up in order to avoid possible deadlocks in local, or through the network.
                 * If the initial executor returns on this units, it will simply leave this SIPU.
                 */
                AppLoggers.Traffic.info(s"(SIPU $reference): Turns out that this unit's executor is blocked. All injections of the executor (${executor.thread.getName}) are being transferred to this thread.")
                true
            case other                    =>
                throw new IllegalThreadStateException(s"Could not inject packet into SIPU: unit's executor is in a dead state (${other.name().toLowerCase()}).")
        }
    } finally accessLock.unlock()

    def getCurrentOrdinal: Int = -1

    private def pollNext(): PacketDownload = {
        val minQueue = unitContentLock.synchronized {
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
            minQueue
        }
        if (minQueue == null) return null //there are no such elements, return null
        minQueue.poll()
    }

    private def injectAll(duringSleep: Boolean): Unit = {
        val currentWorker = WorkerPools.currentWorker
        executor = currentWorker
        for (unit <- chain) {
            unit.join()
            if (executor ne currentWorker) {
                //work of the initial executor thread has been stolen by another thread (see #post)
                return //abort
            }
        }
        // Only the executor thread can run here
        //deserializing all packets that are added in the queue
        var next = pollNext()
        while (next != null) {
            if (executor ne currentWorker)
                return
            deserializeAndInject(next)
            next = pollNext()
        }
        if (executor ne currentWorker)
            return
        //everything deserialized, now realising this deserialization unit.
        accessLock.lock()
        try {
            AppLoggers.Traffic.trace(s"Releasing current unit ${injectable.trafficPath.mkString("/", "/", "")}...")
            if (!duringSleep) executor = null
            joinTaskController.wakeupAllTasks()
            joiningThreads.foreach(LockSupport.unpark)
            joiningThreads.clear()
        } finally accessLock.unlock()
    }

    /**
     * Will make the current thread wait until this unit have terminated all his deserialisation / injection work.
     * */
    def join(): Unit = try {
        accessLock.lock()
        if (executor == null) return //the current IPU is not processing any injection
        AppLoggers.Traffic.debug(s"(SIPU $reference): Waiting for unit to end injections before continuing")
        val currentThread = Thread.currentThread()
        val currentTask   = WorkerPools.currentTask.orNull
        if (currentTask == null && executor != null) {
            joiningThreads += currentThread
            accessLock.unlock()
            LockSupport.park()
            accessLock.lock()
            joiningThreads -= currentThread
        } else {
            AppLoggers.Traffic.trace(s"Pausing task, waiting for '${executor.thread.getName}' to finish.")
            joiningThreads += currentThread
            accessLock.unlock()
            joinTaskController.pauseTask()
            accessLock.lock()
            joiningThreads -= currentThread
        }
    } finally accessLock.unlock()

    private def deserializeAndInject(result: PacketDownload): Unit = {
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

        private val queueContentLock = new ReentrantLock()
        private val queue            = new PriorityQueue[PacketDownload](((a, b) => a.ordinal - b.ordinal): Comparator[PacketDownload])
        private var currentOrdinal   = 0

        var lastUpdate: Long = 0

        def nonEmpty: Boolean = queue.size() > 0

        def size: Int = queue.size()

        def offer(packet: PacketDownload): Unit = {
            queueContentLock.lock()
            queue.offer(packet)
            lastUpdate = System.currentTimeMillis()
            queueContentLock.unlock()
        }

        def poll(): PacketDownload = try {
            queueContentLock.lock()
            val result = queue.poll()
            if (result == null) return null

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
                    queueContentLock.unlock()
                    return pollNext()
                }
                if (refocusShift > 0) {
                    Debugger.push(SIPURectifyStep(reference, currentOrdinal, expectedOrdinal))
                    AppLoggers.Traffic.debug(s"(SIPU $reference): Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal. This unit will wait for the remaining $diff packets before handling other packets.")
                    queueContentLock.unlock()
                    refocusingLocker.pauseTask()
                    queueContentLock.lock()
                    refocusShift = 0
                    Debugger.pop()
                }
                val peek = queue.peek()
                if ((peek ne null) && peek.ordinal == resultOrd)
                    throw UnexpectedPacketException(s"in channel ${injectable.reference}: received packet with same ordinal after refocus.")
                queue.offer(result)
                queueContentLock.unlock()
                return pollNext()
            }
            currentOrdinal = expectedOrdinal
            AppLoggers.Traffic.trace(s"(SIPU $reference): current ordinal is now $currentOrdinal.")
            result
        } finally if (queueContentLock.isHeldByCurrentThread) queueContentLock.unlock()

    }

    class SIPUTaskController extends SimpleTaskController {
        override protected def createControlTicket(pauseCondition: => Boolean): Unit = {
            if (!pauseCondition) return
            super.createControlTicket(pauseCondition)
        }
    }

}