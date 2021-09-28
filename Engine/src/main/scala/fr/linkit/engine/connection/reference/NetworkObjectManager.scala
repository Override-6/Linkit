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

package fr.linkit.engine.connection.reference

import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.{RequestPacketBundle, RequestPacketChannel}
import fr.linkit.api.connection.reference.presence.ObjectPresenceType._
import fr.linkit.api.connection.reference.presence.{NetworkPresenceHandler, ObjectPresenceType}
import fr.linkit.api.connection.reference.{NetworkReferenceLocation, ObjectManager}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.reference.presence.{ExternalNetworkPresence, InternalNetworkPresence}

import scala.collection.mutable

abstract class NetworkObjectManager[R <: AnyRef, L <: NetworkReferenceLocation[R]](channel: RequestPacketChannel)
        extends NetworkPresenceHandler[L] with ObjectManager[R, L] {

    private val externalPresences = mutable.HashMap.empty[L, ExternalNetworkPresence[R, L]]
    private val internalPresences = mutable.HashMap.empty[L, InternalNetworkPresence[R, L]]

    channel.addRequestListener(handleBundle)

    override def isPresentOnEngine(engineId: String, location: L): Boolean = {
        externalPresences(location).getPresenceFor(engineId) eq PRESENT
    }

    def askIfPresent(engineId: String, location: L): Boolean = {
        channel
                .makeRequest(ChannelScopes.include(engineId))
                .addPacket(AnyRefPacket(location))
                .submit()
                .nextResponse
                .nextPacket[BooleanPacket]
    }

    def bindListener(location: L, listener: ExternalNetworkPresence[R, L]): Unit = {
        if (externalPresences.contains(location))
            throw new IllegalArgumentException(s"A listener is already bound for location '$location'.")
        externalPresences.put(location, listener)
    }

    override def registerLocation(location: L): Unit = {
        if (!internalPresences.contains(location)) {
            internalPresences(location) = new InternalNetworkPresence[R, L](this, location)
        } else {
            internalPresences(location).setPresent()
        }
    }

    override def unregisterLocation(location: L): Unit = {
        val opt = internalPresences.get(location)
        if (opt.isDefined) {
            opt.get.setNotPresent()
            internalPresences -= location
        }
    }

    def informPresence(engineId: String, location: L, presence: ObjectPresenceType): Unit = {
        channel
                .makeRequest(ChannelScopes.include(engineId))
                .addPacket(AnyRefPacket((location, presence)))
                .submit()
    }

    private def handleBundle(bundle: RequestPacketBundle): Unit = {
        val responseSubmitter = bundle.responseSubmitter
        val request           = bundle.packet
        request.nextPacket[Packet] match {
            case AnyRefPacket(location: L) =>
                val response = isLocationReferenced(location)
                responseSubmitter
                        .addPacket(BooleanPacket(response))
                        .submit()

            case AnyRefPacket((location: L, presence: ObjectPresenceType)) =>
                val listener = externalPresences(location)
                val senderId = bundle.coords.senderID
                presence match {
                    case NOT_PRESENT => listener.onObjectRemoved(senderId)
                    case PRESENT     => listener.onObjectSet(senderId)
                    case NEVER_ASKED => throw new IllegalArgumentException("Received invalid 'NEVER_ASKED' Presence Type event")
                }
        }
    }

    def isPresent(l: L): Boolean

    private def isLocationReferenced(location: L): Boolean = {
        val opt = internalPresences.get(location)
        if (opt.isEmpty) {
            val presence = new InternalNetworkPresence[R, L](this, location)
            internalPresences(location) = presence
            val present = isPresent(location)
            if (present)
                presence.setPresent()
            present
        } else {
            opt.get.isPresent
        }
    }

}
