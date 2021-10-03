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

package fr.linkit.engine.gnom.reference

import fr.linkit.api.gnom.packet.{Packet, PacketCoordinates}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.reference.presence.ObjectPresenceType._
import fr.linkit.api.gnom.reference.presence.{NetworkPresenceHandler, ObjectNetworkPresence, ObjectPresenceType}
import fr.linkit.api.gnom.reference.traffic.{ObjectManagementChannel, TrafficInterestedNOL}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectLinker, NetworkObjectReference}
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.reference.presence.{ExternalNetworkPresence, InternalNetworkPresence}

import scala.collection.mutable

abstract class AbstractNetworkObjectLinker[O <: NetworkObject[OR], OR <: NetworkObjectReference, LR <: OR](channel: ObjectManagementChannel)
        extends NetworkPresenceHandler[OR] with NetworkObjectLinker[OR, O] with TrafficInterestedNOL[LR]{

    private val internalPresences = mutable.HashMap.empty[OR, InternalNetworkPresence[OR]] //What other engines thinks about current engine references states
    private val externalPresences = mutable.HashMap.empty[OR, ExternalNetworkPresence[OR]] //What current engine thinks about other engines references states

    channel.addRequestListener(handleBundle)

    override def isPresentOnEngine(engineId: String, location: OR): Boolean = {
        externalPresences(location).getPresenceFor(engineId) eq PRESENT
    }

    def askIfPresent(engineId: String, location: OR): Boolean = {
        channel
                .makeRequest(ChannelScopes.include(engineId))
                .addPacket(AnyRefPacket(location))
                .submit()
                .nextResponse
                .nextPacket[BooleanPacket]
    }

    def bindListener(location: OR, listener: ExternalNetworkPresence[OR]): Unit = {
        if (externalPresences.contains(location))
            throw new IllegalArgumentException(s"A listener is already bound for location '$location'.")
        externalPresences.put(location, listener)
    }

    override def registerLocation(location: OR): Unit = {
        if (!internalPresences.contains(location)) {
            internalPresences(location) = new InternalNetworkPresence[OR](this, location)
        } else {
            internalPresences(location).setPresent()
        }
    }

    override def unregisterLocation(location: OR): Unit = {
        val opt = internalPresences.get(location)
        if (opt.isDefined) {
            opt.get.setNotPresent()
            internalPresences -= location
        }
    }

    def informPresence(engineId: String, location: OR, presence: ObjectPresenceType): Unit = {
        channel
                .makeRequest(ChannelScopes.include(engineId))
                .addPacket(AnyRefPacket((location, presence)))
                .submit()
    }

    private def handleBundle(bundle: RequestPacketBundle): Unit = {
        val responseSubmitter = bundle.responseSubmitter
        val request           = bundle.packet
        request.nextPacket[Packet] match {
            case AnyRefPacket(location: OR) =>
                val response = isLocationReferenced(location)
                responseSubmitter
                        .addPacket(BooleanPacket(response))
                        .submit()

            case AnyRefPacket((location: OR, presence: ObjectPresenceType)) =>
                val listener = externalPresences(location)
                val senderId = bundle.coords.senderID
                presence match {
                    case NOT_PRESENT => listener.onObjectRemoved(senderId)
                    case PRESENT     => listener.onObjectSet(senderId)
                    case NEVER_ASKED => throw new IllegalArgumentException("Received invalid 'NEVER_ASKED' Presence Type event")
                }
        }
    }

    def isPresent(l: OR): Boolean

    private def isLocationReferenced(location: OR): Boolean = {
        val opt = internalPresences.get(location)
        if (opt.isEmpty) {
            val presence = new InternalNetworkPresence[OR](this, location)
            internalPresences(location) = presence
            val present = isPresent(location)
            if (present)
                presence.setPresent()
            present
        } else {
            opt.get.isPresent
        }
    }

    override def findObjectLocation(coordsOrigin: PacketCoordinates, ref: O): Option[OR] = {
        val loc = findObjectLocation(ref)
        if (loc.isEmpty)
            return ???
        ???
    }

    override def isObjectReferencedOnCurrent(ref: O): Boolean = findObjectLocation(ref).isDefined

    override def isObjectReferencedOnEngine(engineID: String, ref: O): Boolean = {
        findObjectPresence(ref).exists(_.getPresenceFor(engineID) eq PRESENT)
    }

    override def findObjectPresence(ref: O): Option[ObjectNetworkPresence] = {
        findObjectLocation(ref).flatMap(externalPresences.get)
    }

}
