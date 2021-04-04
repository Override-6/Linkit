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
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.traffic.ParallelInjection.{PacketBuffer, PacketInjectionNode}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId

import java.nio.BufferOverflowException
import scala.collection.mutable.ArrayBuffer

class ParallelInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection {

    private val buff                 = new PacketBuffer
    private val pins                 = ArrayBuffer.empty[PacketInjectionNode]
    @volatile private var processing = false

    override def attachPin(@workerExecution callback: Packet => Unit): Unit = {
        val pin = new PacketInjectionNode(callback)
        pins += pin
        AppLogger.debug(s"Pin attached ! [$coordinates]-($pin => $pins)")
    }

    def insert(packet: Packet): Unit = {
        if (buff.containsNumber(packet))
            return

        buff.insert(packet)
    }

    def processRemainingPins(): Unit = {
        processing = true
        for (i <- pins.indices) {
            buff.process(pins(i))
        }
        processing = false
    }

    override def isProcessing: Boolean = processing
}

object ParallelInjection {

    @volatile private var totalProcess = 0
    private val BuffLength             = 100

    private class PacketInjectionNode(callback: Packet => Unit) {

        private val marks = new Array[Int](BuffLength)
        private var i     = 0

        def makeProcess(packet: Packet): Unit = {
            if (!marks.contains(packet.number)) {
                marks(i) = packet.number
                AppLogger.debug(s"${currentTasksId} <> PROCESSING $packet ($totalProcess / ${packet.number})")
                totalProcess += 1
                callback(packet)
                i += 1
            }
        }

        def foreachUnmarked(f: Packet => Unit, buff: Array[Packet]): Unit = {
            buff.foreach(packet => {
                if (!marks.contains(packet.number))
                    f(packet)
            })
        }

        override def toString: String = s"PacketInjectionNode(${i}, ${marks.mkString("Array(", ", ", ")")})"
    }

    class PacketBuffer {

        private val buff                  = new Array[Packet](BuffLength)
        @volatile private var insertCount = 0

        def containsNumber(packet: Packet): Boolean = {
            val packetNumber = packet.number

            for (packet <- buff) {
                if (packet == null)
                    return false //we reached the last element of this buffer.

                if (packet.number == packetNumber)
                    return true
            }
            false
        }

        def insert(packet: Packet): Unit = buff.synchronized {
            var index        = 0
            val packetNumber = packet.number
            AppLogger.debug(s"${currentTasksId} <> Inserting $packet ($packetNumber) in buffer ${buff.mkString("Array(", ", ", ")")}")
            while (index < buff.length) {
                val indexPacket = buff(index)
                if (indexPacket == null || (indexPacket eq packet)) {
                    AppLogger.debug(s"${currentTasksId} <> Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    insertCount += 1
                    buff(index) = packet
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
                            insertCount += 1
                            return
                        }
                        nextIndex += 1
                    }
                    AppLogger.debug(s"Insertion done ! ${buff.mkString("Array(", ", ", ")")}")
                    insertCount += 1
                    return
                }

                index += 1
            }
            //If the while loop ends, this means that the packet could not be inserted because
            //All indexes of the buffer are taken,
            //or it was inserted, but it threw away another packet that was at the footer
            //of the buffer. So this buffer is overflowed.
            throw new BufferOverflowException()
        }

        def process(pin: PacketInjectionNode): Unit = {
            var validatedInsertCount = insertCount
            var i                    = 0

            def rectifyConcurrentInsertion(): Unit = {
                val packetNumber     = buff(i).number
                val nextPacketNumber = buff(i + 1).number

                if (packetNumber < nextPacketNumber) {
                    i += 1 //The insertion has been done after current packet index. So we just need to continue.
                    validatedInsertCount = insertCount //We have rectified insert count shift
                    return
                }

                //Insertions were done before the current packet's index
                //So we will process all inserted packet before continuing.
                pin.foreachUnmarked(pin.makeProcess, buff)
                //Synchronize buffer in order to prone any insertion during this operation.
                buff.synchronized {
                    i += (insertCount - validatedInsertCount)
                    validatedInsertCount = insertCount //We have rectified insert count shift
                }
            }

            while (i <= buff.length) {
                val packet = buff(i)
                if (packet == null)
                    return //We reached the last element of this buffer.

                pin.makeProcess(packet)
                if (validatedInsertCount != insertCount) {
                    rectifyConcurrentInsertion()
                } else {
                    i += 1
                }
            }
        }

        override def toString: String = buff.mkString("PacketBuffer(", ", ", ")")
    }

}
