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

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.CacheSearchBehavior.{GET_OR_CRASH, GET_OR_OPEN, GET_OR_WAIT}
import fr.linkit.api.gnom.cache.traffic.handler.{AttachHandler, ContentHandler}
import fr.linkit.api.gnom.cache.{CacheContent, CacheOpenException, CacheSearchBehavior}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, Submitter}
import fr.linkit.api.gnom.persistence.context.Deconstructive
import fr.linkit.api.gnom.packet.traffic.PacketInjectableStore
import fr.linkit.engine.gnom.cache.AbstractSharedCacheManager.SystemCacheRange
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{ObjectPacket, StringPacket}
import fr.linkit.engine.gnom.packet.fundamental.ValPacket.IntPacket
import fr.linkit.engine.gnom.packet.fundamental.{EmptyPacket, RefPacket}
import fr.linkit.engine.gnom.persistence.context.Persist

import scala.util.control.Breaks.{break, breakable}

final class SharedCacheOriginManager @Persist()(family: String,
                                                network: Network,
                                                store: PacketInjectableStore) extends AbstractSharedCacheManager(family, network, store) with Deconstructive {

    override val ownerID: String = network.connection.currentIdentifier

    override def deconstruct(): Array[Any] = Array(family, network, store)

    override def handleRequest(requestBundle: RequestPacketBundle): Unit = {
        val coords        = requestBundle.coords
        val requestPacket = requestBundle.packet
        val response      = requestBundle.responseSubmitter
        println(s"HANDLING REQUEST $requestPacket, $coords")
        requestPacket.nextPacket[Packet] match {
            case IntPacket(cacheID)                           => handleContentRetrievalRequest(requestBundle, cacheID)
            case ObjectPacket((id: Int, cacheType: Class[_])) => handlePreCacheOpeningRequest(id, cacheType, coords.senderID, response)
            case unknown                                      => throw UnexpectedPacketException(s"Unknown packet $unknown.")
        }
    }

    override protected def preCacheOpenChecks(cacheID: Int, cacheType: Class[_]): Unit = {
        //DO Nothing, This cache is the origin, it can do whatever he wants, so no checks have to be performed here.
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
    override def retrieveCacheContent(cacheID: Int, behavior: CacheSearchBehavior): Option[CacheContent] = {
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

        LocalCachesStore.getCache(cacheID) match {
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
                    case _                          => acceptRequest //There is no attach handler set, the cache is free to accept any thing
                    case Some(_) if isSystemCache   => acceptRequest //System Caches have free to access caches content.
                    case Some(value: AttachHandler) =>
                        val engine = network.findEngine(senderID).getOrElse {
                            failRequest(s"Unknown engine '$senderID'.")
                        }
                        value.inspectEngine(engine, cacheType).fold(acceptRequest)(failRequest)
                }
        }
    }

    private def handleContentRetrievalRequest(requestBundle: RequestPacketBundle, cacheID: Int): Unit = breakable {
        val coords   = requestBundle.coords
        val request  = requestBundle.packet
        val response = requestBundle.responseSubmitter

        val senderID: String = coords.senderID
        val behavior         = request.getAttribute[CacheSearchBehavior]("behavior").get //TODO orElse throw an exception
        println(s"RECEIVED CONTENT REQUEST FOR IDENTIFIER $cacheID REQUESTOR : $senderID")
        println(s"Behavior = $behavior")

        def failRequest(msg: String): Nothing = {
            response.addPacket(StringPacket(msg))
                    .submit()
            break
        }

        def sendContent(content: Option[CacheContent]): Unit = {
            response.addPacket(RefPacket[Option[CacheContent]](content)).submit()
        }

        def handleContentNotAvailable(): Unit = {
            behavior match {
                case GET_OR_CRASH =>
                    failRequest(s"Requested cache of identifier '$cacheID' is not opened or isn't handled by this connection.")
                case GET_OR_WAIT  =>
                    //If the requester is not the owner, wait the owner to open the cache.
                    if (senderID != ownerID) {
                        channel.storeBundle(requestBundle)
                        println(s"Await open ($cacheID)...")
                        return
                    }
                    //The sender is the owner : this class must create the cache content.
                    sendContent(None)
                case GET_OR_OPEN  =>
                    sendContent(None)
            }
        }

        LocalCachesStore.getCache(cacheID)
                .fold(handleContentNotAvailable()) { storedCache =>
                    val content       = storedCache.getContent
                    val isSystemCache = SystemCacheRange contains cacheID
                    storedCache.channel.getHandler match {
                        case Some(_) if isSystemCache                    => sendContent(content)
                        case None                                        => sendContent(content) //There is no handler, the engine is by default accepted.
                        case Some(handler: ContentHandler[CacheContent]) =>

                            val engine = network.findEngine(senderID).getOrElse {
                                failRequest(s"Engine not found: $senderID. (manager engine: $currentIdentifier)")
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
