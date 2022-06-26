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

import fr.linkit.api.gnom.cache.sync.contract.BasicInvocationRule
import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketBundle, PacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.{Worker, WorkerPools}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.internal.concurrency.pool.{EquilibratedWorkerController, SimpleWorkerController}

import java.lang.Thread.State._
import java.util.{Comparator, PriorityQueue}
import scala.annotation.switch
import scala.collection.mutable.ListBuffer

class SequentialInjectionProcessorUnit(injectable: PacketInjectable) extends InjectionProcessorUnit {
    
    private final val queue            = new PriorityQueue[PacketDownload](((a, b) => a.ordinal - b.ordinal): Comparator[PacketDownload])
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]
    private final var executor: Worker = _
    
    private final val joinLocker                           = new SimpleWorkerController()
    private final val refocusingLocker                     = new EquilibratedWorkerController()
    @volatile private final var waitingForRefocus: Boolean = false
    
    private final var currentOrdinal: Int = 0
    
    override def post(result: PacketDownload): Unit = {
        AppLoggers.Persistence.trace(s"SIPU: adding packet injection for channel '${injectable.reference}'. ord: ${result.ordinal} Current unit executor: $executor. packet queue length = ${queue.size}")
        
        queue.synchronized {
            queue.offer(result)
            if (waitingForRefocus && result.ordinal == currentOrdinal + 1) {
                refocusingLocker.wakeupAnyTask()
                waitingForRefocus = false
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
                    AppLoggers.Persistence.info(s"SIPU: Turns out that the executor is sleeping, waking up executor ($executor) to inject remaining packets.")
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
                        //thread is blocked, let's transfer all the remaining deserialisation / injection work to this thread.
                        //This operation has been made up in order to avoid possible deadlocks in local, or through the network.
                        //If the initial executor is notified or unparked, it will simply give up this SIPU.
                        AppLoggers.Persistence.info(s"SIPU: Turns out that the executor is blocked. All injections of the executor ($executor) are transferred to this thread.")
                    //don't return and let the thread enter in the deserialization process, when the initial executor will get unblocked, the
                    
                    case other =>
                        throw new IllegalThreadStateException(s"Could not inject packet into SIPU: unit's executor is ${other.name().toLowerCase()}.")
                }
            }
            executor = currentWorker
        }
        
        deserializeAll(false)
    }
    
    def getCurrentOrdinal: Int = currentOrdinal
    
    private def nextResult(): PacketDownload = {
        val result = queue.poll()
        if (result == null)
            return null
        val queueLength     = queue.size
        val resultOrd       = result.ordinal
        val expectedOrdinal = currentOrdinal + 1
        if (resultOrd != expectedOrdinal) {
            val diff = resultOrd - expectedOrdinal
            if (diff <= 0) {
                throw UnexpectedPacketException(s"for channel ${injectable.reference}: Received packet with ordinal '$resultOrd', while current ordinal is $expectedOrdinal : a packet has already been handled with ordinal number $resultOrd")
            }
            waitingForRefocus = true
            if (queueLength != queue.size) { //the queue was updated, maybe the remaining packets has been added
                waitingForRefocus = false
                queue.offer(result)
                return nextResult()
            }
            if (waitingForRefocus) {
                AppLoggers.Persistence.debug(s"SIPU: Head of queue ordinal is $diff ahead expected ordinal of $expectedOrdinal. This unit will wait for the remaining $diff packets before handling other packets.")
                refocusingLocker.pauseTask()
                waitingForRefocus = false
            }
            queue.offer(result)
            return nextResult()
        }
        currentOrdinal = expectedOrdinal
        AppLoggers.Debug.trace(s"SIPU: for ${injectable.reference}, current ordinal is now $currentOrdinal.")
        result
    }
    
    private def deserializeAll(duringSleep: Boolean): Unit = {
        if (duringSleep) {
            println(queue.size())
            ""
        }
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
        while (!queue.isEmpty) {
            if (executor ne currentWorker)
                return
            deserializeNextResult()
        }
        if (executor ne currentWorker)
            return
        //everything deserialized, now realising this deserialization unit.
        currentTask.synchronized {
            AppLoggers.Persistence.trace(s"Releasing current unit...")
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
        AppLoggers.Persistence.debug(s"SIPU: Waiting for chained unit (${injectable.reference}) to end injections before continuing")
        val currentTask = WorkerPools.currentTask.orNull
        if (currentTask == null && executor != null)
            this.synchronized(wait())
        else if (executor != null) {
            AppLoggers.Persistence.trace(s"Pausing task, waiting for '$executor' to finish.")
            joinLocker.pauseTask()
        }
    }
    
    private def deserializeNextResult(): Unit = {
        val result = nextResult()
        if (result == null) return
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