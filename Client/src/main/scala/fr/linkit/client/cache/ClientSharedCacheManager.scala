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

package fr.linkit.client.cache

import fr.linkit.api.gnom.cache.{CacheContent, CacheNotAcceptedException, CacheOpenException, CacheSearchMethod}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.client.network.ClientSideNetwork
import fr.linkit.engine.gnom.cache.AbstractSharedCacheManager
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{ObjectPacket, StringPacket}
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.gnom.packet.fundamental.{EmptyPacket, RefPacket}
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.debug.{Debugger, RequestStep, ResponseStep}

final class ClientSharedCacheManager @Persist()(family : String,
                                                network: ClientSideNetwork,
                                                omc    : ObjectManagementChannel,
                                                store  : PacketInjectableStore) extends AbstractSharedCacheManager(family, network, omc, store) with Deconstructible {

    private            val serverNameTag = network.serverNameTag
    @transient private val ownerScope    = prepareScope(ChannelScopes.apply(serverNameTag))

    override def deconstruct(): Array[Any] = Array(family, serverNameTag, network, store)

    override def retrieveCacheContent(cacheID: Int, behavior: CacheSearchMethod): Option[CacheContent] = {
        AppLoggers.GNOM.trace(s"retrieve cache content id $cacheID ($family)")
        Debugger.push(RequestStep("cache content retrieval", s"retrieve cache content of cache $cacheID of cache family $family", serverNameTag, channel.reference))
        val request = channel
                .makeRequest(ownerScope)
                .putAttribute("behavior", behavior)
                .addPacket(IntPacket(cacheID))
                .submit()

        val response = request.nextResponse
        Debugger.pop()
        response.nextPacket[Packet] match {
            case StringPacket(errorMsg)               =>
                throw new CacheOpenException(s"Could not open cache '$cacheID' in shared cache manager <$family>. Received error message from server: $errorMsg")
            case ref: RefPacket[Option[CacheContent]] =>
                ref.value
        }
    }

    override protected def remoteCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit = {
        Debugger.push(RequestStep("check if cache can open", s"ensure that cache $family/$cacheID of type '${cacheType.getSimpleName}' is accepted by server.", serverNameTag, channel.reference))

        channel.makeRequest(ownerScope)
                .addPacket(ObjectPacket((cacheID, cacheType)))
                .submit()
                .nextResponse
                .nextPacket[Packet] match {
            case EmptyPacket =>
            // OK, the cache is not open or is open and the given cacheType
            // is assignable and was accepted by the AttachHandler of the owner's cache handler.
            case StringPacket(msg: String) =>
                // The cache could not be accepted
                // (for any reason. Maybe because cacheType is not assignable with the other caches,
                // or because the AttachHandler of the cache refused the connection.)
                throw new CacheNotAcceptedException(s"server refused cache connection: " + msg)
        }
        Debugger.pop()
    }

    override def handleRequest(requestBundle: RequestPacketBundle): Unit = {
        val response = requestBundle.responseSubmitter
        AppLoggers.GNOM.error(s"RECEIVED REQUEST $requestBundle IN DISTANT MANAGER.")
        val msg =
            s"""This request can't be processed by this engine.
               | The request must be send to the engine that hosts the manager.
               | (Current Identifier = $currentIdentifier)""".stripMargin
        response.putAttribute("errorMsg", msg)
        response.submit()
        AppLoggers.GNOM.error(msg)
    }

}
