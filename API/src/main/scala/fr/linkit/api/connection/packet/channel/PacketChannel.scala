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

package fr.linkit.api.connection.packet.channel

import fr.linkit.api.connection.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.linkit.api.connection.packet.{PacketAttributesPresence, PacketBundle}
import fr.linkit.api.local.system.JustifiedCloseable

trait PacketChannel extends PacketInjectable with JustifiedCloseable with PacketAttributesPresence {

    def storeBundle(bundle: PacketBundle): Unit

    def injectStoredBundles(): Unit

    def getParent: Option[PacketChannel]

}
