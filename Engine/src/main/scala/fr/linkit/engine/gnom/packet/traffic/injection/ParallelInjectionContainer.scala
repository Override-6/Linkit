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

package fr.linkit.engine.gnom.packet.traffic.injection

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.InjectionContainer
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionController
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.SimplePacketBundle

import scala.collection.mutable

class ParallelInjectionContainer extends InjectionContainer {

    private val processingInjections = new mutable.LinkedHashMap[Array[Int], ParallelInjection]

    override def makeInjection(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): PacketInjectionController = {
        makeInjection(SimplePacketBundle(packet, attributes, coordinates))
    }

    override def makeInjection(bundle: PacketBundle): PacketInjectionController = {
        val packet      = bundle.packet
        val number      = packet.number
        val coordinates = bundle.coords
        //AppLogger.debug(s"${currentTasksId} <> $number -> CREATING INJECTION FOR BUNDLE $bundle")
        val path = coordinates.path

        val injection = this.synchronized {
            processingInjections.get(path) match {
                case Some(value) if value.canAcceptMoreInjection =>
                    //AppLogger.error(s"${currentTasksId} <> $number -> INJECTION IS AVAILABLE, ADDING PACKET.")
                    value
                case _                                           =>
                    //AppLogger.error(s"${currentTasksId} <> $number -> INJECTION DOES NOT EXISTS, CREATING IT.")
                    val injection = new ParallelInjection(path)
                    processingInjections.put(path, injection)
                    injection
            }
        }

        injection.insert(bundle)
        injection
    }

    def removeInjection(injection: PacketInjectionController): Unit = {
        processingInjections.remove(injection.injectablePath)
    }

}

