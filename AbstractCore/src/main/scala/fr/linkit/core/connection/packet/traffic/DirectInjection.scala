/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.packet.traffic

import fr.linkit.api.connection.packet.traffic.PacketInjection
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.traffic.DirectInjection.{PacketBuffer, PacketCallback}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTaskId

import java.nio.BufferOverflowException
import scala.collection.mutable.ListBuffer

class DirectInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection {

    private val buff      = new PacketBuffer
    private val callbacks = ListBuffer.empty[PacketCallback]

    override def process(callback0: Packet => Unit): Unit = {
        val callback = new PacketCallback(callback0)
        callbacks.synchronized {
            callbacks += callback
        }
        buff.process(callback)
    }

    def inject(packet: Packet): Unit = {
        if (buff.contains(packet))
            return

        buff.insert(packet)
        val currentThread = Thread.currentThread()
        callbacks.foreach(callback => {
            if (callback.threadHandler == null) {
                callback.threadHandler = currentThread
                buff.process(callback)
            }
        })
    }

}

object DirectInjection {

    @volatile private var totalProcess = 0

    private class PacketCallback(callback: Packet => Unit) {

        var threadHandler: Thread = _
        private val marks = new Array[Int](10)
        private var i     = 0

        def apply(packet: Packet): Unit = {
            if (!marks.contains(packet.number)) {
                callback(packet)
                marks(i) = packet.number
                i += 1
            }
        }
    }

    class PacketBuffer {

        private val buff             = new Array[Packet](10)
        @volatile private var length = 0

        def contains(packet: Packet): Boolean = {
            val packetNumber = packet.number
            for (i <- 0 until length) {
                if (buff(i).number == packetNumber)
                    return true
            }
            false
        }

        def insert(packet: Packet): Unit = {
            var index        = 0
            val packetNumber = packet.number
            AppLogger.debug(s"${currentTaskId} <> Inserting $packet in buffer ${buff.mkString("Array(", ", ", ")")}")
            while (index < buff.length) {
                val indexPacket = buff(index)
                if (indexPacket == null) {
                    buff(index) = packet
                    AppLogger.debug(s"${currentTaskId} <> Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    length += 1
                    return
                }
                val indexPacketNumber = indexPacket.number
                if (packetNumber < indexPacketNumber) {
                    var nextIndex = index + 1
                    //println(s"nextIndex = ${nextIndex}")
                    //println(s"buff = ${buff.mkString("Array(", ", ", ")")}")
                    while (nextIndex < buff.length) {
                        val nextPacket = buff(nextIndex)
                        buff(index) = packet
                        buff(nextIndex) = indexPacket

                        if (nextPacket == null) {
                            AppLogger.debug(s"Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                            length += 1
                            return
                        }
                        nextIndex += 1
                    }
                    AppLogger.debug(s"Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    length += 1
                    return
                }

                index += 1
            }
            //if the while ends, this means that the packet could not be inserted
            //or it was inserted, but it threw away another packet that was at the footer
            //of the buffer.
            throw new BufferOverflowException()
        }

        def process(action: PacketCallback): Unit = {
            var count = 0
            var i     = 0
            while (count < length) {
                val packet = buff(i)
                totalProcess += 1
                AppLogger.debug(s"${currentTaskId} <> PROCESSING $packet ($totalProcess / ${packet.number})")
                if (packet != null) {
                    action(packet)
                    count += 1
                }
                i += 1
            }
        }
    }

}
