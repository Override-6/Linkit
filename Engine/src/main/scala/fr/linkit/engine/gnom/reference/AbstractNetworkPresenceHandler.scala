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

import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.reference.presence.ObjectPresenceType._
import fr.linkit.api.gnom.reference.presence.{NetworkObjectPresence, NetworkPresenceHandler, ObjectPresenceType}
import fr.linkit.api.gnom.reference.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.reference.presence.{ExternalNetworkObjectPresence, InternalNetworkObjectPresence}

import scala.collection.mutable

abstract class AbstractNetworkPresenceHandler[R <: NetworkObjectReference](channel: ObjectManagementChannel)
    extends NetworkPresenceHandler[R] with TrafficInterestedNPH {

    private val currentIdentifier = channel.traffic.currentIdentifier
    //What other engines thinks about current engine references states
    private val internalPresences = mutable.HashMap.empty[R, InternalNetworkObjectPresence[R]]
    //What current engine thinks about other engines references states
    private val externalPresences = mutable.HashMap.empty[R, ExternalNetworkObjectPresence[R]]

    @inline
    def getPresence(ref: R): NetworkObjectPresence = {
        externalPresences.getOrElseUpdate(ref, new ExternalNetworkObjectPresence[R](this, ref))
    }

    override def isPresentOnEngine(engineId: String, ref: R): Boolean = {
        getPresence(ref).getPresenceFor(engineId) eq PRESENT
    }

    def findPresence(ref: R): Option[NetworkObjectPresence] = {
        Some(getPresence(ref))
    }

    private[reference] def askIfPresent(engineId: String, location: R): Boolean = {
        val traffic = channel.traffic
        //fixme quick wobbly fix
        if (traffic != null && traffic.currentIdentifier == engineId)
            return isLocationReferenced(location)
        AppLogger.debug(s"Asking if $location is present on engine: $engineId")
        val isPresent = channel
            .makeRequest(ChannelScopes.include(engineId))
            .addPacket(EmptyPacket)
            .putAttribute(ReferenceAttributeKey, location)
            .submit()
            .nextResponse
            .nextPacket[BooleanPacket]
        AppLogger.debug(s"is any network object registered at $location on engine $engineId ? $isPresent")
        isPresent
    }

    private[reference] def informPresence(enginesId: Array[String], location: R, presence: ObjectPresenceType): Unit = {
        channel
            .makeRequest(ChannelScopes.include(enginesId: _*))
            .addPacket(AnyRefPacket(presence))
            .putAttribute(ReferenceAttributeKey, location)
            .submit()
    }

    def bindListener(location: R, listener: ExternalNetworkObjectPresence[R]): Unit = {
        if (externalPresences.contains(location))
            throw new IllegalArgumentException(s"A listener is already bound for location '$location'.")
        externalPresences.put(location, listener)
    }

    protected def registerReference(ref: R): Unit = ref.getClass.synchronized {
            AppLogger.info(s"Registering Network Object reference $ref (${System.identityHashCode(ref)}).")
            var presence: InternalNetworkObjectPresence[R] = null
            if (!internalPresences.contains(ref)) {
                presence = new InternalNetworkObjectPresence[R](this, ref)
                internalPresences(ref) = presence
            } else {
                presence = internalPresences(ref)
            }
            presence.setPresent()
            val opt = externalPresences.get(ref)
            if (opt.isDefined)
                opt.get.onObjectSet(currentIdentifier)
        }

    protected def unregisterReference(ref: R): Unit = {
        val opt = internalPresences.get(ref)
        if (opt.isDefined) {
            opt.get.setNotPresent()
            internalPresences -= ref
        }
        val otp2 = externalPresences.get(ref)
        if (otp2.isDefined)
            otp2.get.onObjectRemoved(currentIdentifier)
    }

    protected def handleBundle(bundle: LinkerRequestBundle): Unit = {
        val responseSubmitter = bundle.responseSubmitter
        val request           = bundle.packet
        val reference: R      = bundle.linkerReference.asInstanceOf[R]
        val pct = request.nextPacket[Packet]
        val senderId = bundle.coords.senderID
        AppLogger.warn(s"handling bundle, request from $senderId. packet : $pct")
        pct match {
            case EmptyPacket =>
                val isPresent = isLocationReferenced(reference)
                AppLogger.warn(s"is $reference present in this engine : $isPresent")
                responseSubmitter
                    .addPacket(BooleanPacket(isPresent))
                    .submit()
                val presence = internalPresences(reference)
                presence.setPresenceFor(senderId, if (isPresent) PRESENT else NOT_PRESENT)
                AppLogger.warn(s"presence information sent and modified for '$senderId' to ${if (isPresent) PRESENT else NOT_PRESENT}")

            case AnyRefPacket(presence: ObjectPresenceType) =>
                val listener = externalPresences(reference)
                presence match {
                    case NOT_PRESENT => listener.onObjectRemoved(senderId)
                    case PRESENT     => listener.onObjectSet(senderId)
                    case NEVER_ASKED => throw new IllegalArgumentException("Received invalid 'NEVER_ASKED' Presence Type event")
                }
        }
    }

    def findObject(location: R): Option[NetworkObject[_ <: R]]

    private def isLocationReferenced(location: R): Boolean = {
        val opt = internalPresences.get(location)
        if (opt.isEmpty) {
            val presence = new InternalNetworkObjectPresence[R](this, location)
            internalPresences(location) = presence
            val present = findObject(location).isDefined
            if (present) {
                presence.setPresent()
            }
            present
        } else {
            opt.get.isPresent
        }
    }

}
