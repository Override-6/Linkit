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

package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.ObjectSerializer

case class DedicatedPacketCoordinates(injectableID: Int, targetID: String, senderID: String) extends PacketCoordinates {

    override def toString: String = s"DedicatedPacketCoordinates(channelId: $injectableID, targetID: $targetID, senderID: $senderID)"

    def reversed(): DedicatedPacketCoordinates = DedicatedPacketCoordinates(injectableID, senderID, targetID)

    override def determineSerializer(cachedWhitelist: Array[String], raw: ObjectSerializer, cached: ObjectSerializer): ObjectSerializer = {
        if (cachedWhitelist.contains(targetID)) cached else raw
    }
}
