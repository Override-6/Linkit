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

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.cache.sync.invokation.UsageRMIRulesAgreement
import fr.linkit.engine.gnom.packet.{AbstractAttributesPresence, SimplePacketAttributes}

class AgreementScope(override val writer: PacketWriter, network: Network, agreement: RMIRulesAgreement) extends AbstractAttributesPresence with ChannelScope {

    private val currentIdentifier = writer.currentIdentifier

    override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
        defaultAttributes.drainAttributes(attributes)
        if (agreement.isAcceptAll) {
            writer.writeBroadcastPacket(packet, attributes, agreement.discardedEngines :+ currentIdentifier)
        } else {
            writer.writePacket(packet, attributes, agreement.acceptedEngines.filterNot(_.equals(currentIdentifier)))
        }
    }

    override def sendToAll(packet: Packet): Unit = sendToAll(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, attributes: PacketAttributes, targetIDs: Array[String]): Unit = {
        throw new UnsupportedOperationException("Not supported.")
    }

    override def sendTo(packet: Packet, targetIDs: Array[String]): Unit = sendTo(packet, SimplePacketAttributes.empty, targetIDs)

    override def areAuthorised(identifiers: Array[String]): Boolean = {
        !agreement.discardedEngines.containsSlice(identifiers)
    }

    override def canConflictWith(scope: ChannelScope): Boolean = {
        true //This scope may not coexists with another channel on the same injectableID
    }

    override def shareWriter[S <: ChannelScope](factory: ChannelScope.ScopeFactory[S]): S = factory(writer)

    def foreachAcceptedEngines(action: String => Unit): Unit = {
        if (agreement.isAcceptAll) {
            val engines = network.listEngines
            engines.foreach { engine =>
                val id = engine.identifier
                if (!agreement.discardedEngines.contains(id))
                    action(id)
            }
        } else {
            agreement.acceptedEngines.foreach(action)
        }
    }

}

object AgreementScope {

    def apply(agreement: UsageRMIRulesAgreement, network: Network): ScopeFactory[AgreementScope] = {
        writer => new AgreementScope(writer, network, agreement)
    }
}
