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

package fr.linkit.api.connection.packet

case class BroadcastPacketCoordinates(override val injectableID: Int,
                                      override val senderID: String,
                                      discardTargets: Boolean,
                                      targetIDs: String*) extends PacketCoordinates {

    override def foreachConcernedTargets(action: String => Unit): Unit = {

    }

    def listDiscarded(alreadyConnected: Seq[String]): Seq[String] = {
        if (discardTargets)
            targetIDs
        else alreadyConnected.filterNot(targetIDs.contains)
    }

    def getDedicated(target: String): DedicatedPacketCoordinates = {
        if (targetIDs.contains(target) == discardTargets) {
            throw new IllegalArgumentException(s"These coordinates does not target $target (discardTargets = $discardTargets).")
        }

        DedicatedPacketCoordinates(injectableID, target, senderID)
    }

    override def toString: String = s"BroadcastPacketCoordinates(injectableID: $injectableID, senderID: $senderID, discardTargets: $discardTargets, targetIDs: $targetIDs)"
}

