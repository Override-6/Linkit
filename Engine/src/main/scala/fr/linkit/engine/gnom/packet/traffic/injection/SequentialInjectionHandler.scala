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

import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionHandler
import fr.linkit.api.gnom.persistence.ObjectDeserializationResult

import scala.collection.mutable

/**
 * Injection handler that will process each packet deserialization in one thread
 * in order to synchronize the deserialization and injection.
 * */
class SequentialInjectionHandler(traffic: PacketTraffic) extends PacketInjectionHandler {

    /*
    * All working units are stored in this map
    * */
    private val handlerThreads = mutable.HashMap.empty[Array[Int], DeserializationUnit]

    /**
     * This implementation will ensure that the resulting packet is deserialized in the right order.
     * */
    override def deserializeAndInject(result: ObjectDeserializationResult): Unit = {
        val path = result.coords.path
        val node = traffic.getNode(path)
        val unit = handlerThreads.get(path) match {
            case None       => createUnit(path)
            case Some(unit) =>
                //Maybe the thread just ended his deserialization task between now and the handlerThreads.get()
                if (unit.isDead) unit.synchronized {
                    handlerThreads.remove(unit.key)
                    createUnit(path)
                } else {
                    unit
                }
        }
        //posting the result
        unit.post(result)
    }

    private def createUnit(path: Array[Int]): DeserializationUnit = {
        val unit = new DeserializationUnit(path)
        handlerThreads.put(path, unit)
        unit
    }

    /*
    * A Deserialization unit is attributed to one thread which is responsible for deserializing and injecting the packet
    * */
    class DeserializationUnit(val key: Array[Int]) {

        private var dead     = false
        private val executor = Thread.currentThread()
        private val queue    = mutable.Queue.empty[ObjectDeserializationResult]

        def isDead: Boolean = dead

        def post(result: ObjectDeserializationResult): Unit = {
            queue.enqueue(result)
            val methodExecutor = Thread.currentThread()
            if (methodExecutor ne executor)
                return

            // Only the executor thread can run here
            while (queue.nonEmpty) {
                val result = queue.dequeue()
                result.makeDeserialization()
                traffic.processInjection(result)
            }
            dead = true
            this.synchronized {
                handlerThreads.remove(key) //removes self
            }
        }
    }

}


