/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.packet

import fr.linkit.api.gnom.network.{IdentifierTag, Network, NetworkFriendlyEngineTag, UniqueTag}

//MAINTAINED
case class BroadcastPacketCoordinates(override val path: Array[Int],
                                      override val senderID: UniqueTag with NetworkFriendlyEngineTag,
                                      discardTargets: Boolean,
                                      targetIDs: Seq[UniqueTag with NetworkFriendlyEngineTag]) extends PacketCoordinates {

    override def toString: String = s"BroadcastPacketCoordinates(${path.mkString("/")}, $senderID, $discardTargets, $targetIDs)"

    def listDiscarded(network: Network): Seq[UniqueTag with NetworkFriendlyEngineTag] = {
        if (discardTargets)
            targetIDs
        else network.listEngines
                .map(e => IdentifierTag(e.name))
                .filterNot(targetIDs.contains)
    }

    def getDedicated(network: Network, target: UniqueTag with NetworkFriendlyEngineTag): DedicatedPacketCoordinates = {
        if (targetIDs.exists(tag => network.findEngine(tag).exists(_.isTagged(tag))) == discardTargets) {
            throw new IllegalArgumentException(s"These coordinates does not target $target (discardTargets = $discardTargets).")
        }
        DedicatedPacketCoordinates(path, target, senderID)
    }
}