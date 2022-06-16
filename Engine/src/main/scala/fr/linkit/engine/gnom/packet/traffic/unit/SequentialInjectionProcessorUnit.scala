/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.unit

import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketBundle, PacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.{Worker, WorkerPools}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController

import java.lang.Thread.State._
import scala.annotation.switch
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SequentialInjectionProcessorUnit(injectable: PacketInjectable) extends InjectionProcessorUnit {
    
    private final val queue            = mutable.PriorityQueue.empty[PacketDownload] { case (a, b) => a.ordinal - b.ordinal }
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]
    private final var executor: Worker = _
    
    private final val joinLocker                 = new SimpleWorkerController()
    private final val refocusingLocker           = new SimpleWorkerController()
    private final var waitingForRefocus: Boolean = false
    
    private final var currentOrdinal: Int = 0
    
    override def post(result: PacketDownload): Unit = {
        AppLoggers.Persistence.trace(s"SIPU: adding packet injection for channel '${injectable.reference}'. ord: ${result.ordinal} Current unit executor: $executor. packet queue length = ${queue.size}")
        
        queue.synchronized {
            queue.enqueue(result)
            if (waitingForRefocus && result.ordinal == currentOrdinal + 1)
                refocusingLocker.wakeupAnyTask()
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
                    AppLoggers.Persistence.info(s"SIPU: Turns out that the executor is sleeping, waking up executor ($executor) to inject remaining packets.")
                    executor.runWhileSleeping(deserializeAll(true, injectable))
                    return
                }
                val threadState = executor.thread.getState
                (threadState: @switch) match {
                    case RUNNABLE | TIMED_WAITING =>
                        //thread is running (or is partially waiting),
                        // just add the packet and let the executor wakeup
                        return
                    case WAITING | BLOCKED        =>
                        //thread is blocked, let's transfer all the remaining deserialisation / injection work to this thread.
                        //This operation has been made up in order to avoid possible deadlocks in local, or through the network.
                        //If the initial executor is notified or unparked, it will simply give up this SIPU.
                        AppLoggers.Persistence.info(s"SIPU: Turns out that the executor is blocked. All injections of the executor ($executor) are transferred to this thread.")
                    //don't return and let the thread enter in the deserialization process, when the initial executor will get unblocked, the
                    
                    case other =>
                        throw new IllegalThreadStateException(s"Could not inject packet into SIPU: unit's executor is ${other.name().toLowerCase()}.")
                }
            }
        }
        
        executor = currentWorker
        deserializeAll(false, injectable)
    }
    
    private def deserializeAll(duringSleep: Boolean, injectable: PacketInjectable): Unit = {
        // If any of the chained SIPU is processing,
        // the current unit will wait until the whole chain is complete.
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
        while (queue.nonEmpty) {
            if (executor ne currentWorker)
                return
            deserializeNextResult(injectable)
        }
        if (executor ne currentWorker)
            return
        //everything deserialized, now realising this deserialization unit.
        currentTask.synchronized {
            AppLoggers.GNOM.trace(s"Releasing current unit...")
            if (!duringSleep) executor = null
            joinLocker.wakeupAllTasks()
            this.synchronized(this.notifyAll())
        }
    }
    
    /**
     * Will make the current thread wait until this unit have terminated all his deserialisation / injection work.
     * */
    def join(): Unit = {
        if (executor == null) return //the current IPU is not processing any injection
        AppLoggers.GNOM.debug(s"SIPU: Waiting for chained unit to end injections before continuing")
        val currentTask = WorkerPools.currentTask.orNull
        if (currentTask == null && executor != null)
            this.synchronized(wait())
        else if (executor != null) {
            AppLoggers.GNOM.trace(s"Pausing task, waiting for '$executor' to finish.")
            joinLocker.pauseTask()
        }
    }
    
    private def nextResult(): PacketDownload = {
        val (result, queueLength) = queue.synchronized(queue.dequeue(), queue.size)
        val resultOrd             = result.ordinal
        val expectedOrdinal       = currentOrdinal + 1
        println(Thread.currentThread() + ": received packet in " + injectable.reference + s" (ordinal: $resultOrd, expected: $expectedOrdinal)")
        if (resultOrd != expectedOrdinal) {
            val diff = resultOrd - expectedOrdinal
            if (diff <= 0)
                throw UnexpectedPacketException(s"for channel ${injectable.reference}: Received packet with ordinal '$resultOrd', while expected ordinal is $expectedOrdinal : a packet has already been handled with ordinal number $resultOrd")
            AppLoggers.Persistence.debug(s"SIPU: Head of queue ordinal is $diff above expected ordinal of $expectedOrdinal. This unit will wait for the $diff packets remaining before handling other packets.")
            queue.synchronized {
                waitingForRefocus = true
                if (queueLength != queue.size) //the queue was updated, maybe the remaining packets has been added
                    return nextResult()
            }
            refocusingLocker.pauseTask()
            return nextResult()
        }
        currentOrdinal = expectedOrdinal
        result
    }
    
    def deserializeNextResult(injectable: PacketInjectable): Unit = {
        val result = queue.synchronized {
            if (queue.isEmpty) return
            nextResult()
        }
        AppLoggers.Persistence.trace(s"handling packet deserialization and injection (ord: ${result.ordinal})")
        result.makeDeserialization()
        val bundle = new PacketBundle {
            override val packet    : Packet            = result.packet
            override val attributes: PacketAttributes  = result.attributes
            override val coords    : PacketCoordinates = result.coords
        }
        injectable.inject(bundle)
    }
    
    def chainWith(unit: SequentialInjectionProcessorUnit): Unit = {
        chain += unit
    }
    
}