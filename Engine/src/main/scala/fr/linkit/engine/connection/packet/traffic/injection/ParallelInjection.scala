/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.traffic.injection

import fr.linkit.api.connection.packet.traffic.PacketInjectable
import fr.linkit.api.connection.packet.traffic.injection.{PacketInjection, PacketInjectionController}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.traffic.injection.ParallelInjection.{PacketBuffer, PacketInjectionNode}

import java.nio.BufferOverflowException
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}

class ParallelInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection with PacketInjectionController {

    private val buff                         = new PacketBuffer
    private val pins                         = ArrayBuffer.empty[PacketInjectionNode]
    @volatile private var processing         = false
    @volatile private var performedPinAttach = false

    override def attachPin(@workerExecution callback: (Packet, PacketAttributes) => Unit): Unit = {

        val pin = new PacketInjectionNode(callback)
        pins += pin
        AppLogger.vDebug(s"Pin attached ! ($hashCode)(${pin.hashCode()})[$coordinates]")
        if (processing)
            buff.process(pin)
    }

    override def attachPinPacket(callback: Packet => Unit): Unit = {
        attachPin((packet, _) => callback(packet))
    }

    override def processRemainingPins(): Unit = {

        AppLogger.vDebug(s"processRemainingPins ($hashCode)[$coordinates]")
        for (i <- pins.indices) {
            buff.process(pins(i))
        }
    }

    override def isProcessing: Boolean = this.synchronized {
        processing
    }

    override def performPinAttach(injectables: Iterable[PacketInjectable]): Boolean = {
        var tookEffect = false
        if (!performedPinAttach) {
            injectables.foreach(_.inject(this))
            performedPinAttach = true
            tookEffect = true
        }
        tookEffect
    }

    override def process(action: => Unit): Unit = {

        this.synchronized {
            if (processing)
                throw InjectionAlreadyProcessingException(this, "Attempted to call PacketInjectionController#process while another process is handled.")
            processing = true
        }
        action
        processing = false
    }

    def insert(packet: Packet, attributes: PacketAttributes): Unit = {

        if (buff.containsNumber(packet))
            return

        buff.insert(packet, attributes)
    }

}

object ParallelInjection {

    @volatile private var totalProcess = 0
    private val BuffLength             = 100

    private class PacketInjectionNode(callback: (Packet, PacketAttributes) => Unit) {

        private val marks = new Array[Int](BuffLength)
        private var i     = 0

        def makeProcess(packet: Packet, attributes: PacketAttributes): Unit = {
            val number = packet.number
            AppLogger.vDebug(s"PACKET NUMBER ${number} - ($hashCode)")
            val canProcess = marks.synchronized {
                val contains = marks.contains(number)
                marks(i) = number
                !contains
            }
            if (canProcess) {
                i += 1
                totalProcess += 1
                AppLogger.vDebug(s"${currentTasksId} <> PROCESSING ($number) $packet with attributes $attributes ($totalProcess / ${number})")
                callback(packet, attributes)
            } else {
                AppLogger.vDebug(s"PACKET ABORTED ${number} - ($hashCode)")
            }
        }

        def foreachUnmarked(f: (Packet, PacketAttributes) => Unit, buff: Array[(Packet, PacketAttributes)]): Unit = breakable {
            buff.foreach(tuple => {
                if (tuple == null)
                    break() //we have reached the end of the tuple.
                if (!marks.contains(tuple._1.number))
                    f(tuple._1, tuple._2)
            })
        }

        override def toString: String = s"PacketInjectionNode(${i}, ${marks.mkString("Array(", ", ", ")")})"
    }

    class PacketBuffer {

        private val buff                  = new Array[(Packet, PacketAttributes)](BuffLength)
        @volatile private var insertCount = 0

        def containsNumber(packet: Packet): Boolean = {
            val packetNumber = packet.number

            for ((packet, _) <- buff) {
                if (packet == null)
                    return false //we reached the last element of this buffer.

                if (packet.number == packetNumber)
                    return true
            }
            false
        }

        def insert(packet: Packet, attributes: PacketAttributes): Unit = buff.synchronized {
            var index        = 0
            val packetNumber = packet.number
            AppLogger.vDebug(s"${currentTasksId} <> Inserting $packet ($packetNumber) with attributes $attributes in buffer")
            while (index < buff.length) {
                val pair = buff(index)
                if (pair == null || (pair._1 eq packet)) {
                    insertCount += 1
                    buff(index) = (packet, attributes)
                    AppLogger.vDebug(s"${currentTasksId} <> Insertion done ! (${buff.mkString("Array(", ", ", ")")})")
                    return
                }
                val indexPacket       = pair._1
                val indexPacketNumber = indexPacket.number
                if (packetNumber < indexPacketNumber) {
                    var nextIndex = index + 1
                    //println(s"nextIndex = ${nextIndex}")
                    //println(s"buff = ${buff.mkString("Array(", ", ", ")")}")
                    while (nextIndex < buff.length) {
                        val pair = buff(nextIndex)
                        buff(index) = (packet, attributes)
                        buff(nextIndex) = (indexPacket, attributes)

                        if (pair == null) {
                            AppLogger.vDebug(s"Insertion done !")
                            insertCount += 1
                            return
                        }
                        nextIndex += 1
                    }
                    AppLogger.vDebug(s"Insertion done !")
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
                AppLogger.vDebug(s"BUFFER HAS BEEN MODIFIED ${buff.mkString("Array(", ", ", ")")}")
                if (i == buff.length - 1)
                    return
                val packetNumber = buff(i)._1.number
                val next         = buff(i + 1)
                if (next == null)
                    return
                val nextPacketNumber = next._1.number

                if (packetNumber < nextPacketNumber) {
                    //Synchronize buffer in order to prone any insertion during this operation.
                    buff.synchronized {
                        i += 1 //The insertion has been done after current packet index. So we just need to continue.
                        validatedInsertCount = insertCount //We have rectified insert count shift
                        AppLogger.vDebug(s"RECTIFICATION DONE.")
                    }
                    return
                }

                //Insertions were done before the current packet's index
                //So we will process all inserted packet before continuing.
                pin.foreachUnmarked(pin.makeProcess, buff)
                //Synchronize buffer in order to prone any insertion during this operation.
                buff.synchronized {
                    i += (insertCount - validatedInsertCount)
                    validatedInsertCount = insertCount //We have rectified insert count shift
                    AppLogger.vDebug(s"RECTIFICATION DONE.")
                }
            }

            //AppLogger.vDebug(s"PROCESSING ALL PACKETS OF BUFFER ${buff.mkString("Array(", ", ", ")")}")

            while (i <= buff.length) {
                val pair = buff(i)
                if (pair == null)
                    return //We reached the last element of this buffer.

                pin.makeProcess(pair._1, pair._2)
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
