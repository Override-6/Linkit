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

package fr.linkit.api.connection.packet.traffic.injection

import fr.linkit.api.connection.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet, PacketAttributes}
import fr.linkit.api.local.concurrency.workerExecution

/**
 * A Packet Injection is a handler class which contains a bunch of packets
 * with common coordinates and which are barely injected at the same time.
 *
 * Packet Injection is explicitly given to [[PacketInjectable]] by a [[PacketTraffic]] in order to
 * notify them that there is a bunch of packet to inject.
 *
 * All packets contained in a PacketInjection are bound to a common [[PacketInjection#coordinates]].
 * The coordinates must always target the current support identifier, but may vary for injectableID and senderID.
 * */
trait PacketInjection {

    /**
     * Coordinates that all packets contained in the injection are targeting.
     * */
    val injectablePath: Array[Int]

}
