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

package fr.linkit.engine.gnom.packet.traffic.injection

import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.injection.InjectionProcessorUnit
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketBundle, PacketCoordinates}
import fr.linkit.api.gnom.persistence.ObjectDeserializationResult
import fr.linkit.api.internal.concurrency.{Worker, WorkerPools}
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController

import scala.annotation.StaticAnnotation
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SequentialInjectionProcessorUnit() extends InjectionProcessorUnit {

    private final var executor: Worker = _
    private final val queue            = mutable.PriorityQueue.empty[ObjectDeserializationResult] { case (a, b) => a.ordinal - b.ordinal }
    private final val locker           = new SimpleWorkerController()
    private final val chain            = ListBuffer.empty[SequentialInjectionProcessorUnit]

    override def post(result: ObjectDeserializationResult, injectable: PacketInjectable): Unit = {
        val methodExecutor = WorkerPools.currentWorker
        //AppLogger.debug(s"posting serialization result in sipu, executor = $executor")
        queue.synchronized {
            queue.enqueue(result)
        }
        this.synchronized {
            if (executor != null && methodExecutor != executor) {
                //the current thread is not the thread in charge of deserializing
                //and injecting packets for this InjectionProcessorUnit.
                if (executor.isSleeping) {
                    //If the thread that is in charge of the deserialization / injection process is doing nothing
                    //then force it to deserialize all remaining packets
                    executor.runSpecificTask(deserializeAll(injectable))
                }
                return
            }
            executor = methodExecutor
        }
        deserializeAll(injectable)
    }

    private def deserializeAll(injectable: PacketInjectable): Unit = {
        // If any of the chained SIPU is processing,
        // the current unit will wait until the whole chain is complete.
        chain.foreach(_.join())
        // Only the executor thread can run here
        //deserializing all packets that are added in the queue
        while (queue.nonEmpty) {
            deserializeNextResult(injectable)
        }
        //everything deserialized, now realising this deserialization unit.
        this.synchronized {
            executor = null
            locker.wakeupAllTasks()
            this.notifyAll()
        }
    }

    /**
     * Will make the current thread wait until this unit have terminated all his deserialisation work.
     * */
    def join(): Unit = {
        val currentTask = this.synchronized {
            if (executor == null)
                return //the current IPU is not processing any injection
            WorkerPools.currentTask.orNull
        }
        if (currentTask == null && executor != null)
            this.synchronized(wait())
        else if (executor != null)
            locker.pauseTask()
    }

    def deserializeNextResult(injectable: PacketInjectable): Unit = {
        val result = queue.synchronized {
            var result = queue.dequeue()
            if (result == null)
                return

            if (queue.nonEmpty && queue.head.ordinal < result.ordinal) {
                val lastResult = result
                result = queue.dequeue()
                queue.enqueue(lastResult)
            }
            result
        }
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