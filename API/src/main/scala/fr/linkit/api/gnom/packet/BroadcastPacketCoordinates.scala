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

package fr.linkit.api.gnom.packet

case class BroadcastPacketCoordinates(override val path: Array[Int],
                                      override val senderID: String,
                                      discardTargets: Boolean,
                                      targetIDs: Seq[String]) extends PacketCoordinates {

    override def toString: String = s"BroadcastPacketCoordinates(${path.mkString("/")}, $senderID, $discardTargets, $targetIDs)"

    def listDiscarded(alreadyConnected: Seq[String]): Seq[String] = {
        if (discardTargets)
            targetIDs
        else alreadyConnected.filterNot(targetIDs.contains)
    }

    def getDedicated(target: String): DedicatedPacketCoordinates = {
        if (targetIDs.contains(target) == discardTargets) {
            throw new IllegalArgumentException(s"These coordinates does not target $target (discardTargets = $discardTargets).")
        }

        DedicatedPacketCoordinates(path, target, senderID)
    }
}

