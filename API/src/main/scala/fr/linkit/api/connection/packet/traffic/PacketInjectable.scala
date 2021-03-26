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

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.JustifiedCloseable

trait PacketInjectable extends JustifiedCloseable {
    val identifier: Int
    val ownerID: String
    /**
     * The traffic handler that is directly or not injecting the packets
     * */
    val traffic: PacketTraffic

    @workerExecution
    def inject(injection: PacketInjection): Unit

    def canInjectFrom(identifier: String): Boolean

    /**
     * Creates a PacketInjectable that will be handled by this packet channel. <br>
     * The packet channel can send and receive packets from the target.  <br>
     * In other words, this will create a sub channel that is bound to a specific relay.  <br>
     * If the sub channel receives a packet, the parent collector will handle the packet as well if the
     * children authorises it
     *
     * @param scopes the identifiers that the injectable will listen to
     * @param factory the factory that wil determine the kind of PacketInjectable that will be used.
     * @param transparent if true, the children is set as a 'transparent' injectable.
     *                    That means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * */
    def subInjectable[C <: PacketInjectable](scopes: Array[String],
                                             factory: PacketInjectableFactory[C],
                                             transparent: Boolean): C

    def subInjectable[C <: PacketInjectable](scope: String,
                                             factory: PacketInjectableFactory[C],
                                             transparent: Boolean = false): C = {
        subInjectable(Array(scope), factory, transparent)
    }

    /**
     * The children behaves and is handled like the scopped [[subInjectable]] method describes it.
     * The exception here is that it will be linked with the same scope of the parent.
     *
     * @param factory the factory that wil determine the kind of PacketInjectable that will be used.
     * @param transparent if true, the children is set as a 'transparent' injectable.
     *                    That means that if a packet is injected into the subChannel, the parent will
     *                    handle the packet as well
     * */
    def subInjectable[C <: PacketInjectable](factory: PacketInjectableFactory[C],
                                             transparent: Boolean): C

}
