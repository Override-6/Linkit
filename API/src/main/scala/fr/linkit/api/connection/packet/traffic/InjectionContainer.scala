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

package fr.linkit.api.connection.packet.traffic
import fr.linkit.api.connection.packet.traffic.injection.{PacketInjection, PacketInjectionController}
import fr.linkit.api.connection.packet.{Bundle, DedicatedPacketCoordinates, Packet, PacketAttributes}

trait InjectionContainer {

    def makeInjection(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): PacketInjectionController

    def makeInjection(bundle: Bundle): PacketInjectionController

    def isInjecting(injection: PacketInjection): Boolean

}