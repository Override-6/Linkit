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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.gnom.packet.{PacketAttributesPresence, PacketBundle}
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.JustifiedCloseable

trait PacketInjectable extends TrafficPresence with JustifiedCloseable with PacketAttributesPresence {

    val ownerID: String
    /**
     * The traffic handler that is directly or not injecting the packets
     * */
    val traffic: PacketTraffic

    val store: PacketInjectableStore

    @workerExecution
    def inject(bundle: PacketBundle): Unit

    def canInjectFrom(identifier: String): Boolean

}
