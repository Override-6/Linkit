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

package fr.linkit.api.connection.packet

import fr.linkit.api.connection.packet.serialization.Serializer

case class BroadcastPacketCoordinates(injectableID: Int, senderID: String, discardTargets: Boolean, targetIDs: String*) extends PacketCoordinates {

    override def determineSerializer(array: Array[String], raw: Serializer, cached: Serializer): Serializer = {
        //if there is a target that is not whitelisted, use the raw serializer
        if (targetIDs.forall(array.contains(_)))
            cached
        else raw
    }

    def listDiscarded(alreadyConnected: Seq[String]): Seq[String] = {
        if (discardTargets)
            targetIDs
        else alreadyConnected.filterNot(targetIDs.contains)
    }

    override def toString: String = s"BroadcastPacketCoordinates(injectableID: $injectableID, senderID: $senderID, discardTargets: $discardTargets, targetIDs: $targetIDs)"
}
