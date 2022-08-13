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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.traffic.handler.{CacheAttachHandler, CacheContentHandler}
import fr.linkit.api.gnom.cache.{CacheContent, CacheOpenException, CacheSearchMethod}
import CacheSearchMethod._
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, Submitter}
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.api.gnom.persistence.context.{Deconstructible, Persist}
import fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.AbstractSharedCacheManager.SystemCacheRange
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{ObjectPacket, StringPacket}
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.gnom.packet.fundamental.{EmptyPacket, RefPacket}

import scala.util.control.Breaks.{break, breakable}

final class SharedCacheOriginManager @Persist()(family: String,
                                                network: Network,
                                                omc: ObjectManagementChannel,
                                                store: PacketInjectableStore) extends AbstractSharedCacheManager(family, network, omc, store) with Deconstructible {

    override val ownerID: String = network.connection.currentIdentifier

    override def deconstruct(): Array[Any] = Array(family, network, store)

    override def handleRequest(requestBundle: RequestPacketBundle): Unit = {
        val coords        = requestBundle.coords
        val requestPacket = requestBundle.packet
        val response      = requestBundle.responseSubmitter
        requestPacket.nextPacket[Packet] match {
            case IntPacket(cacheID)                           => handleContentRetrievalRequest(requestBundle, cacheID)
            case ObjectPacket((id: Int, cacheType: Class[_])) => handlePreCacheOpeningRequest(id, cacheType, coords.senderID, response)
            case unknown                                      => throw UnexpectedPacketException(s"Unknown packet $unknown.")
        }
    }

    override protected def remoteCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit = {
        //DO Nothing, This cache is the origin, it can do whatever he wants, so no checks have to be performed from here.
    }

    /**
     * Retrieves the cache content of a given cache identifier.
     *
     * @param cacheID  the identifier of a cache content that needs to be retrieved.
     * @param behavior the kind of behavior to adopt when retrieving a cache content
     * @return Some(content) if the cache content was retrieved, None if no cache has been found.
     * @throws CacheOpenException if something went wrong during the cache content retrieval (can be affected by behavior parameter)
     * @see [[CacheContent]]
     * */
    override def retrieveCacheContent(cacheID: Int, behavior: CacheSearchMethod): Option[CacheContent] = {
        LocalCachesStore.getContent(cacheID)
    }

    private def handlePreCacheOpeningRequest(cacheID: Int, cacheType: Class[_], senderID: String, response: Submitter[_]): Unit = breakable {
        def acceptRequest: Nothing = {
            response.addPacket(EmptyPacket).submit()
            break
        }

        def failRequest(msg: String): Nothing = {
            response.addPacket(StringPacket(msg)).submit()
            break
        }

        LocalCachesStore.findCache(cacheID) match {
            case None                  =>
                //The cache is accepted because it is not yet opened.
                //TODO Make a "blacklist" or a "whitelist" of caches types that can or cannot be opened on a SharedCacheManager
                acceptRequest
            case Some(registeredCache) =>
                val cacheClass = registeredCache.cache.getClass
                if (!cacheType.isAssignableFrom(cacheClass))
                    failRequest(s"For cache identifier $cacheID: Shared cache of type '${cacheClass.getName}', contained in local storage is not assignable to requested cache '${cacheType.getName}'.")

                val isSystemCache = SystemCacheRange contains cacheID
                registeredCache.channel.getHandler match {
                    case None                            => acceptRequest //There is no attach handler set, the cache is free to accept any thing
                    case Some(_) if isSystemCache        => acceptRequest //System Caches are free to access caches content.
                    case Some(value: CacheAttachHandler) =>
                        val engine = network.findEngine(senderID).getOrElse {
                            failRequest(s"Unknown engine '$senderID'.")
                        }
                        value.inspect(engine, registeredCache.cache.getClass, cacheType).fold(acceptRequest)(failRequest)
                }
        }
    }

    private def handleContentRetrievalRequest(requestBundle: RequestPacketBundle, cacheID: Int): Unit = breakable {
        AppLoggers.GNOM.trace(s"handling content retrieval request (cacheID: $cacheID, family: $family)")
        val coords   = requestBundle.coords
        val request  = requestBundle.packet
        val response = requestBundle.responseSubmitter

        val senderID: String = coords.senderID
        val behavior         = request.getAttribute[CacheSearchMethod]("behavior").get //TODO orElse throw an exception

        def failRequest(msg: String): Nothing = {
            AppLoggers.GNOM.error(s"Could not send cache content to $senderID: $msg")
            response.addPacket(StringPacket(msg))
                    .submit()
            break
        }

        def sendContent(content: Option[CacheContent]): Unit = {
            AppLoggers.GNOM.trace(s"sending cache content (cacheID: $cacheID, family: $family)")
            response.addPacket(RefPacket[Option[CacheContent]](content))
                    .submit()
        }

        def handleContentNotAvailable(): Unit = behavior match {
            case GET_OR_CRASH =>
                failRequest(s"Requested cache of identifier '$cacheID' is not opened or isn't handled by this connection.")
            case GET_OR_WAIT  =>
                //If the requester is not the owner, wait the owner to open the cache.
                if (senderID != ownerID) {
                    channel.storeBundle(requestBundle)
                    return
                }
                //The sender is the owner : this class must create the cache content.
                sendContent(None)
            case GET_OR_OPEN  =>
                sendContent(None)
        }

        LocalCachesStore.findCache(cacheID)
                .fold(handleContentNotAvailable()) { storedCache =>
                    val content       = storedCache.getContent
                    val isSystemCache = SystemCacheRange contains cacheID
                    storedCache.channel.getHandler match {
                        case Some(_) if isSystemCache                         => sendContent(content)
                        case None                                             => sendContent(content) //There is no handler, the engine is by default accepted.
                        case Some(handler: CacheContentHandler[CacheContent]) =>
                            val engine = network.findEngine(senderID).getOrElse {
                                failRequest(s"Engine not found: $senderID. (shared cache manager engine: $currentIdentifier)")
                            }
                            if (handler.canAccessToContent(engine)) {
                                sendContent(content)
                            } else {
                                failRequest(s"Engine $engine can't access to content of cache '$cacheID'.")
                            }
                    }
                }
    }
}