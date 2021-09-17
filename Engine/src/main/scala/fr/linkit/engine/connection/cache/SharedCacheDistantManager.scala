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

package fr.linkit.engine.connection.cache

import fr.linkit.api.connection.cache.{CacheContent, CacheNotAcceptedException, CacheOpenException, CacheSearchBehavior}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.channel.request.RequestPacketBundle
import fr.linkit.api.connection.packet.traffic.PacketInjectableStore
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket.EmptyPacket
import fr.linkit.engine.connection.packet.fundamental.RefPacket.{ObjectPacket, StringPacket}
import fr.linkit.engine.connection.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.connection.packet.fundamental.{EmptyPacket, RefPacket}
import fr.linkit.engine.connection.packet.traffic.ChannelScopes

final class SharedCacheDistantManager(family: String,
                                      override val ownerID: String,
                                      @transient network: Network,
                                      @transient store: PacketInjectableStore) extends AbstractSharedCacheManager(family, network, store) {

    @transient private val ownerScope = prepareScope(ChannelScopes.include(ownerID))

    override def retrieveCacheContent(cacheID: Int, behavior: CacheSearchBehavior): Option[CacheContent] = {
        println(s"Sending request to $ownerID in order to retrieve content of cache number $cacheID")
        val request = channel
            .makeRequest(ownerScope)
            .putAttribute("behavior", behavior)
            .addPacket(IntPacket(cacheID))
            .submit()

        try {
            val response = request.nextResponse
            request.detach()
            response.nextPacket[Packet] match {
                case StringPacket(errorMsg)               => throw new CacheOpenException(errorMsg + s"(this = $ownerID)")
                case ref: RefPacket[Option[CacheContent]] =>
                    val content = ref.value
                    //println(s"Content '$cacheID' received ! ($content)")
                    content
            }
        } catch {
            case e: Throwable =>
                AppLogger.fatal(s"Was executing request (${request.id}) for cache ID '$cacheID'.")
                AppLogger.printStackTrace(e)
                System.exit(1)
                throw null
        }
    }

    override protected def preCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit = {
        channel.makeRequest(ownerScope)
            .addPacket(ObjectPacket((cacheID, cacheType)))
            .submit()
            .nextResponse
            .nextPacket[Packet] match {
            case e: EmptyPacket =>
            // OK, the cache is not open or is open and the given cacheType
            // is assignable and was accepted by the AttachHandler of the owner's cache handler.
            case StringPacket(msg: String) =>
                // The cache could not be accepted
                // (for any reason. Maybe because cacheType is not assignable with the other caches,
                // or because the AttachHandler of the cache refused the connection.)
                throw new CacheNotAcceptedException(s"This message comes from engine $ownerID: " + msg)
        }
    }

    override def handleRequest(requestBundle: RequestPacketBundle): Unit = {
        val response = requestBundle.responseSubmitter
        println(s"HANDLING REQUEST $requestBundle")
        val msg =
            s"""This request can't be processed by this engine.
               | The request must be performed to the engine that hosts the manager.
               | (Current Identifier = $currentIdentifier, host identifier = $ownerID)""".stripMargin
        response.putAttribute("errorMsg", msg)
        response.submit()
    }

}
