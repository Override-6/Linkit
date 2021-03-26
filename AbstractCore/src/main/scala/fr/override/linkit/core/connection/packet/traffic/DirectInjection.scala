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

package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.traffic.PacketInjection
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution

import scala.collection.mutable.ListBuffer

class DirectInjection(override val coordinates: DedicatedPacketCoordinates) extends PacketInjection {

    private val injections = ListBuffer.empty[(Int, Packet)]
    private val injectableID = coordinates.injectableID
    //The thread that created this object
    //will be set as the handler of the injection
    private[traffic] val handlerThread = Thread.currentThread()

    @workerExecution
    override def getPackets: Seq[Packet] = injections.synchronized {
        val packets = Array.from(injections)
            .sorted((x: (Int, Packet), y: (Int, Packet)) => x._1 - y._1)
            .map(_._2)
        //println(s"DISCOVERED PACKETS ${packets.mkString("Array(", ", ", ")")} vs INJECTIONS $injections")
        PacketInjections.currentInjections.remove((injectableID, coordinates.senderID))
        packets
    }

    @workerExecution
    override def mayNotHandle: Boolean = {
        Thread.currentThread() != handlerThread
    }

    def addPacket(packetNumber: Int, packet: Packet): Unit = injections.synchronized {
        injections += ((packetNumber, packet))
    }
}
