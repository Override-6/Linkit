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

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.InjectionContainer
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionControl
import fr.linkit.engine.gnom.packet.SimplePacketBundle

class ParallelInjectionContainer extends InjectionContainer {

    private val processingInjections = new java.util.LinkedHashMap[Array[Int], ParallelInjection]()

    override def makeInjection(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): PacketInjectionControl = {
        makeInjection(SimplePacketBundle(packet, attributes, coordinates))
    }

    override def makeInjection(bundle: PacketBundle): PacketInjectionControl = {
        val coordinates = bundle.coords
        //AppLogger.debug(s"${currentTasksId} <> $number -> CREATING INJECTION FOR BUNDLE $bundle")
        val path        = coordinates.path

        val injection = this.synchronized {
            val value = processingInjections.get(path)
            if (value != null && value.canAcceptMoreInjection)
                value
            else {
                val value = new ParallelInjection(path)
                processingInjections.put(path, value)
                value
            }
        }

        injection.insert(bundle)
        injection
    }

    def removeInjection(injection: PacketInjectionControl): Unit = {
        processingInjections.remove(injection.injectablePath)
    }

}

