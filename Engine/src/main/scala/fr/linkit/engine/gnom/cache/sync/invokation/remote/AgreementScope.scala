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
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{EngineResolver, NetworkFriendlyEngineTag}
import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.traffic.PacketWriter
import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}
import fr.linkit.engine.gnom.cache.sync.invokation.UsageRMIDispatchAgreement
import fr.linkit.engine.gnom.packet.{AbstractAttributesPresence, SimplePacketAttributes}

class AgreementScope(override val writer: PacketWriter,
                     resolver: EngineResolver,
                     agreement: RMIDispatchAgreement) extends AbstractAttributesPresence with ChannelScope {


    val selection = agreement.selection

    override def sendToAll(packet: Packet, attributes: PacketAttributes): Unit = {
        defaultAttributes.drainAttributes(attributes)
        writer.writePackets(packet, attributes, selection)
    }

    override def sendToAll(packet: Packet): Unit = sendToAll(packet, SimplePacketAttributes.empty)

    override def sendTo(packet: Packet, attributes: PacketAttributes, tag: NetworkFriendlyEngineTag): Unit = {
        throw new UnsupportedOperationException("Not supported.")
    }

    override def sendTo(packet: Packet, tag: NetworkFriendlyEngineTag): Unit = sendTo(packet, SimplePacketAttributes.empty, tag)

    override def areAuthorised(tag: NetworkFriendlyEngineTag): Boolean = {
        resolver.isIncluded(tag, selection)
    }

    override def shareWriter[S <: ChannelScope](factory: ChannelScope.ScopeFactory[S]): S = factory(writer)

}

object AgreementScope {

    def apply(agreement: UsageRMIDispatchAgreement, network: Network): ScopeFactory[AgreementScope] = {
        writer => new AgreementScope(writer, network, agreement)
    }
}
