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
import fr.linkit.api.gnom.packet.traffic.injection.{PacketInjection, PacketInjectionControl}
import fr.linkit.api.gnom.packet.{Packet, PacketBundle}
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.traffic.injection.ParallelInjection.PacketBuffer

import java.nio.BufferOverflowException

class ParallelInjection(override val injectablePath: Array[Int]) extends PacketInjection with PacketInjectionControl {

    private var processing = false
    private val buff       = new PacketBuffer
    private var pathIndex  = -1
    private val limit      = injectablePath.length

    override def isProcessing: Boolean = processing

    override def markAsProcessing(): Unit = processing = true

    def insert(bundle: PacketBundle): Unit = {

        if (buff.containsNumber(bundle.packet))
            return

        buff.insert(bundle)
    }

    override def canAcceptMoreInjection: Boolean = {
        buff.canAcceptMoreInjection
    }

    override def nextIdentifier: Int = {
        pathIndex += 1
        injectablePath(pathIndex)
    }

    override def haveMoreIdentifier: Boolean = pathIndex <= limit

    override def process(injectable: PacketInjectable): Unit = buff.process(injectable)
}

object ParallelInjection {

    private val BuffLength = 100

    //TODO This buffer can be optimised : some iterations while searching or inserting can be avoided
    class PacketBuffer {

        private val buff        = new Array[PacketBundle](BuffLength)
        private var insertCount = 0

        def containsNumber(packet: Packet): Boolean = {
            val packetNumber = packet.number
            for (bundle <- buff) {
                if (bundle == null)
                    return false //we reached the last element of this buffer.

                if (bundle.packet.number == packetNumber)
                    return true
            }
            false
        }

        def insert(bundle: PacketBundle): Unit = buff.synchronized {
            var index        = 0
            val packet       = bundle.packet
            val packetNumber = packet.number
            //AppLogger.vDebug(s"${currentTasksId} <> Inserting bundle $bundle")
            while (index < buff.length) {
                val buffBundle = buff(index)
                if (buffBundle == null || (buffBundle eq bundle)) {
                    insertCount += 1
                    buff(index) = bundle
                    //AppLogger.vDebug(s"${currentTasksId} <> Insertion done ! (${buff.mkString("Array(", ", ", ")")})")
                    return
                }
                val buffPacket        = buffBundle.packet
                val indexPacketNumber = buffPacket.number
                if (packetNumber < indexPacketNumber) {
                    var nextIndex = index + 1
                    //AppLogger.vDebug(s"nextIndex = ${nextIndex}")
                    //AppLogger.vDebug(s"buff = ${buff.mkString("Array(", ", ", ")")}")
                    while (nextIndex < buff.length) {
                        val pair = buff(nextIndex)
                        buff(index) = bundle
                        buff(nextIndex) = buffBundle

                        if (pair == null) {
                            //AppLogger.vDebug(s"Insertion done !")
                            insertCount += 1
                            return
                        }
                        nextIndex += 1
                    }
                    //AppLogger.vDebug(s"Insertion done !")
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

        def process(injectable: PacketInjectable): Unit = {
            var validatedInsertCount = insertCount
            var i                    = 0

            def rectifyConcurrentInsertion(): Unit = {
                AppLogger.vDebug(s"BUFFER HAS BEEN MODIFIED ${buff.mkString("Array(", ", ", ")")}")
                if (i == buff.length - 1)
                    return
                val packetNumber = buff(i).packet.number
                val next         = buff(i + 1)
                if (next == null)
                    return
                val nextPacketNumber = next.packet.number

                if (packetNumber < nextPacketNumber) {
                    //Synchronize buffer in order to prone any insertion during this operation.
                    buff.synchronized {
                        i += 1 //The insertion has been done after current packet index. So we just need to continue.
                        validatedInsertCount = insertCount //We have rectified insert count shift
                        AppLogger.vDebug(s"RECTIFICATION DONE.")
                    }
                    return
                }

                //Synchronize buffer in order to prone any insertion during this operation.
                buff.synchronized {
                    i += (insertCount - validatedInsertCount)
                    validatedInsertCount = insertCount //We have rectified insert count shift
                    AppLogger.vDebug(s"RECTIFICATION DONE.")
                }
            }

            //AppLogger.vDebug(s"PROCESSING ALL PACKETS OF BUFFER ${buff.mkString("Array(", ", ", ")")}")

            while (i <= buff.length) {
                val bundle = buff(i)
                if (bundle == null)
                    return //We reached the last element of this buffer.

                injectable.inject(bundle)
                if (validatedInsertCount != insertCount) {
                    rectifyConcurrentInsertion()
                } else {
                    i += 1
                }
            }
        }

        def canAcceptMoreInjection: Boolean = insertCount < BuffLength

        override def toString: String = buff.mkString("PacketBuffer(", ", ", ")")
    }

}
