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

package fr.linkit.api.connection.packet.traffic

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
    val coordinates: DedicatedPacketCoordinates

    /**
     * A Pin is a Packet Injection Node, it is used whenever the implementation decides it,
     * but it must must be called once for each packets.
     * @param callback the action to perform for each packets of this injection.
     *                 The thread that processes this action depends on the implementation, so be aware
     *                 that the callback can be executed in the same thread that attached the pin, or be
     *                 called later.
     * */
    @workerExecution
    def attachPin(@workerExecution callback: (Packet, PacketAttributes) => Unit): Unit

    def attachPinPacket(@workerExecution callback: (Packet) => Unit): Unit

    /**
     * Notifies that all attached pins may be processed. <br>
     * Each callbacks of pins will be called if the implementation
     * decides to do it for the current thread.
     * */
    @workerExecution
    def processRemainingPinsThen(action: => Unit): Unit

    def isProcessing: Boolean

}
