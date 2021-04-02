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
import fr.linkit.core.connection.packet.traffic.DirectInjection.{PacketBuffer, ProcessNode}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import java.nio.BufferOverflowException
import java.util.ConcurrentModificationException
import scala.collection.mutable.ListBuffer

class DirectInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection {

    private val buff  = new PacketBuffer
    private val nodes = ListBuffer.empty[ProcessNode]

    override def process(callback: Packet => Unit): Unit = {
        val node = new ProcessNode(callback)
        nodes.synchronized {
            nodes += node
        }
        buff.process(node)
    }

    def inject(packet: Packet): Unit = {
        if (buff.contains(packet))
            return

        buff.insert(packet)
        val currentThread = Thread.currentThread()
        nodes.foreach(callback => {
            if (callback.threadHandler == null) {
                callback.threadHandler = currentThread
                buff.process(callback)
            }
        })
    }

}

object DirectInjection {

    @volatile private var totalProcess = 0
    private val BuffLength             = 100

    private class ProcessNode(callback: Packet => Unit) {

        var threadHandler: Thread = _
        private val marks = new Array[Int](BuffLength)
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

        private val buff               = new Array[Packet](BuffLength)
        @volatile private var modCount = 0

        def contains(packet: Packet): Boolean = {
            val packetNumber = packet.number

            for (packet <- buff) {
                if (packet == null)
                    return false //we reached the last element of this buffer.

                if (packet.number == packetNumber)
                    return true
            }
            false
        }

        def insert(packet: Packet): Unit = {
            var index        = 0
            val packetNumber = packet.number
            AppLogger.debug(s"${currentTasksId} <> Inserting $packet in buffer ${buff.mkString("Array(", ", ", ")")}")
            while (index < buff.length) {
                val indexPacket = buff(index)
                if (indexPacket == null || (indexPacket eq packet)) {
                    buff(index) = packet
                    AppLogger.debug(s"${currentTasksId} <> Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    modCount += 1
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
                            modCount += 1
                            return
                        }
                        nextIndex += 1
                    }
                    AppLogger.debug(s"Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    modCount += 1
                    return
                }

                index += 1
            }
            //if the while loop ends, this means that the packet could not be inserted because
            //All indexes of the buffer are taken,
            //or it was inserted, but it threw away another packet that was at the footer
            //of the buffer. So this buffer is overflowed.
            throw new BufferOverflowException()
        }

        def process(action: ProcessNode): Unit = {
            var i = 0
            AppLogger.debug(s"${currentTasksId} <> PROCESSING PACKETS OF BUFFER ${buff.mkString("Array(", ", ", ")")}")
            val initialModCount = modCount

            for (packet <- buff) {
                if (packet == null)
                    return //We reached the last element of this buffer.
                totalProcess += 1
                AppLogger.debug(s"${currentTasksId} <> PROCESSING $packet ($totalProcess / ${packet.number})")
                if (packet != null) {
                    action(packet)
                }
                if (initialModCount != modCount) {
                    throw new ConcurrentModificationException()
                }
                i += 1
            }
        }
    }

}
