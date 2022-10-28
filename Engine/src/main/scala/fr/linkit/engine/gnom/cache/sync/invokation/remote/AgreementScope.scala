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

package fr.linkit.engine.gnom.cache.sync.invokation.remote

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIDispatchAgreement
import fr.linkit.api.gnom.network.{Everyone, IdentifierTag, Network, NetworkFriendlyEngineTag}
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.cache.sync.invokation.UsageRMIDispatchAgreement
import fr.linkit.engine.gnom.packet.{AbstractAttributesPresence, SimplePacketAttributes}

import scala.util.chaining.scalaUtilChainingOps

class AgreementScope(override val writer: PacketWriter, network: Network, agreement: RMIDispatchAgreement) extends AbstractAttributesPresence with ChannelScope {

    override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
        defaultAttributes.drainAttributes(attributes)
        val included = agreement.acceptedEngines.toSeq.pipe(set => if (agreement.isAcceptAll) set :+ Everyone else set)
        val excluded = agreement.discardedEngines.toSeq.pipe(set => if (!agreement.isAcceptAll) set :+ Everyone else set)
        writer.writePackets(packet, attributes, included, excluded)
    }

    override def sendToAll(packet: Packet): Unit = sendToAll(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: Array[NetworkFriendlyEngineTag]): Unit = {
        throw new UnsupportedOperationException("Not supported.")
    }

    override def sendTo(packet: Packet, targetIDs: Array[NetworkFriendlyEngineTag]): Unit = sendTo(packet, SimplePacketAttributes.empty, targetIDs)

    override def areAuthorised(tags: Array[NetworkFriendlyEngineTag]): Boolean = {
        val subjects = if (agreement.isAcceptAll) agreement.discardedEngines else agreement.acceptedEngines
        subjects.forall(id => network.findEngine(id).exists(e => tags.exists(e.isTagged)))
    }

    override def shareWriter[S <: ChannelScope](factory: ChannelScope.ScopeFactory[S]): S = factory(writer)

    def foreachAcceptedEngines(action: IdentifierTag => Unit): Unit = {
        if (agreement.isAcceptAll) {
            val engines = network.listEngines
            engines.foreach { engine =>
                val id = IdentifierTag(engine.name)
                if (!agreement.discardedEngines.contains(id))
                    action(id)
            }
        } else {
            agreement.acceptedEngines.foreach(action)
        }
    }

}

object AgreementScope {

    def apply(agreement: UsageRMIDispatchAgreement, network: Network): ScopeFactory[AgreementScope] = {
        writer => new AgreementScope(writer, network, agreement)
    }
}
