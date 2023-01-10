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

package fr.linkit.engine.gnom.referencing.presence

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.referencing.presence.ObjectPresenceState._
import fr.linkit.api.gnom.referencing.presence.{NetworkObjectPresence, NetworkPresenceHandler, ObjectPresenceState}
import fr.linkit.api.gnom.referencing.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.debug.{Debugger, RequestStep}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

abstract class AbstractNetworkPresenceHandler[R <: NetworkObjectReference](parent  : Option[NetworkPresenceHandler[_ >: R]],
                                                                           omc     : ObjectManagementChannel,
                                                                           selector: EngineSelector)
        extends NetworkPresenceHandler[R] with TrafficInterestedNPH {

    //What other engines thinks about current engine presences states
    private val internalPresences = mutable.HashMap.empty[R, InternalNetworkObjectPresence[R]]
    //What current engine thinks about other engines presences states
    private val externalPresences = mutable.HashMap.empty[R, ExternalNetworkObjectPresence[R]]

    import selector._


    @inline
    def getPresence(ref: R): NetworkObjectPresence = getPresence0(ref)

    private def getPresence0(ref: R): ExternalNetworkObjectPresence[R] = {
        if (ref eq null)
            throw new NullPointerException()
        externalPresences.getOrElseUpdate(ref, {
            new ExternalNetworkObjectPresence[R](selector, this, ref)
        })
    }

    override def isPresentOnEngine(engineTag: UniqueTag with NetworkFriendlyEngineTag, ref: R): Boolean = {
        val presence = getPresence(ref)

        val engineNameTag = selector.retrieveNT(engineTag)
        val isPresent     = if (presence.isPresenceKnownFor(engineNameTag)) {
            presence.getPresenceFor(engineNameTag) eq PRESENT
        } else {
            val parent = this.parent.orNull
            (parent == null || ref.asSuper.exists(r => parent.isPresentOnEngine(engineNameTag, casted(r)))) &&
                    (presence.getPresenceFor(engineNameTag) eq PRESENT)
        }
        AppLoggers.GNOM.debug(s"is '$ref' present on engine '${engineNameTag.name}' ? $isPresent")
        isPresent
    }

    private def casted[A](ref: NetworkObjectReference): A = ref.asInstanceOf[A]


    private[referencing] def askIfPresent(engineName: NameTag, reference: R): Boolean = {
        val traffic = omc.traffic
        if (traffic != null && Current <=> engineName)
            return isLocationReferenced(reference)

        val parent          = this.parent.orNull
        val parentIsPresent = parent == null || reference.asSuper.exists(r => parent.isPresentOnEngine(engineName, casted(r)))
        parentIsPresent && {
            AppLoggers.GNOM.debug(s"Asking if $reference is present on engine: $engineName")
            Debugger.push(RequestStep("presence check", s"asking if reference $reference is present on engine $engineName", engineName, omc.reference))
            val isPresent = omc
                    .makeRequest(ChannelScopes.apply(engineName))
                    .addPacket(EmptyPacket)
                    .putAttribute(ReferenceAttributeKey, reference)
                    .submit()
                    .nextResponse
                    .nextPacket[BooleanPacket]
                    .value
            Debugger.pop()
            isPresent
        }
    }

    private[referencing] def informPresence(enginesId: Array[NameTag], location: R, presence: ObjectPresenceState): Unit = {
        omc
                .makeRequest(ChannelScopes(enginesId.toList.foldLeft(Select(Nobody): TagSelection[NetworkFriendlyEngineTag])(_ U _)))
                .addPacket(AnyRefPacket(presence))
                .putAttribute(ReferenceAttributeKey, location)
                .submit()
    }

    override def toString: String = "presence handler " + getClass.getSimpleName

    protected def registerReference(ref: R): Unit = ref.getClass.synchronized {
        AppLoggers.GNOM.trace(s"Registering Network Object reference $ref in presence handler '${this}'.")
        var presence: InternalNetworkObjectPresence[R] = null
        if (!internalPresences.contains(ref)) {
            presence = new InternalNetworkObjectPresence[R](selector, this, ref)
            internalPresences(ref) = presence
        } else {
            presence = internalPresences(ref)
        }
        presence.setPresent()
        val opt = externalPresences.get(ref)
        if (opt.isDefined) {
            opt.get.setToPresent(Current)
            AppLoggers.GNOM.debug(s"Presence set to present for current engine ($Current), at reference $ref")
        }
    }

    protected def unregisterReference(ref: R): Unit = {
        val opt = internalPresences.get(ref)
        if (opt.isDefined) {
            opt.get.setNotPresent()
            internalPresences -= ref
        }
        val otp2 = externalPresences.get(ref)
        if (otp2.isDefined)
            otp2.get.setToNotPresent(Current)
    }

    def injectRequest(bundle: LinkerRequestBundle): Unit = {
        val responseSubmitter = bundle.responseSubmitter
        val request           = bundle.packet
        val reference: R      = bundle.linkerReference.asInstanceOf[R]
        val pct               = request.nextPacket[Packet]
        val senderName        = retrieveNT(bundle.coords.senderTag)
        AppLoggers.GNOM.trace(s"Handling presence request bundle, request from $senderName. packet : $pct")
        pct match {
            case EmptyPacket =>
                val isPresent = isLocationReferenced(reference)
                AppLoggers.GNOM.trace(s"is $reference present in this engine : $isPresent")
                responseSubmitter
                        .addPacket(BooleanPacket(isPresent))
                        .submit()
                val presence = internalPresences(reference)
                presence.setPresenceFor(senderName, if (isPresent) PRESENT else NOT_PRESENT)
                AppLoggers.GNOM.trace(s"presence information sent and modified for '$senderName' to ${if (isPresent) PRESENT else NOT_PRESENT}")

            case AnyRefPacket(presenceType: ObjectPresenceState) =>
                val presence = getPresence0(reference)
                presenceType match {
                    case NOT_PRESENT => presence.setToNotPresent(senderName)
                    case PRESENT     =>
                        presence.setToPresent(senderName)
                        AppLoggers.GNOM.debug(s"Presence set to present for engine $senderName, for reference $reference")
                    case NEVER_ASKED => throw new IllegalArgumentException("Received invalid 'NEVER_ASKED' Presence Type event")
                }
        }
    }

    def findObject(location: R): Option[NetworkObject[_ <: R]]

    private def isLocationReferenced(location: R): Boolean = {
        val opt = internalPresences.get(location)
        if (opt.isEmpty) {
            val presence = new InternalNetworkObjectPresence[R](selector, this, location)
            internalPresences(location) = presence
            val present = findObject(location).isDefined
            if (present) {
                presence.setPresent()
            }
            present
        } else {
            opt.get.isPresentOnCurrent
        }
    }

}