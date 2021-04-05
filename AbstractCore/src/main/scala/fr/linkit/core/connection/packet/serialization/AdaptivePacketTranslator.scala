/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.network.cache.SharedCacheManager
import fr.linkit.api.connection.packet._
import fr.linkit.api.connection.packet.serialization._
import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategyHolder}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.security.BytesHasher
import fr.linkit.core.connection.network.cache.collection.SharedCollection
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class AdaptivePacketTranslator(ownerIdentifier: String, securityManager: BytesHasher) extends PacketTranslator { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel
    override def translate(info: TransferInfo): PacketSerializationResult = {
        val result = AdaptiveSerializer.serialize(info)
        securityManager.hashBytes(result.bytes)
        result
    }

    override def translate(bytes: Array[Byte]): PacketTransferResult = {
        securityManager.deHashBytes(bytes)
        AdaptiveSerializer.deserialize(bytes)
    }

    override def translateCoords(coords: PacketCoordinates, target: String): Array[Byte] = {
        AdaptiveSerializer.serialize(coords, target)
    }

    override def translateAttributes(attribute: PacketAttributes, target: String): Array[Byte] = {
        AdaptiveSerializer.serialize(attribute, target)
    }

    override def translatePacket(packet: Packet, target: String): Array[Byte] = {
        AdaptiveSerializer.serialize(packet, target)
    }

    override def attachStrategy(strategy: SerialStrategy[_]): Unit = {
        AdaptiveSerializer.attachStrategy(strategy)
    }

    override val signature: Array[Byte] = new Array(3)

    override def drainAllStrategies(holder: StrategyHolder): Unit = AdaptiveSerializer.rawSerializer.drainAllStrategies(holder)

    def updateCache(manager: SharedCacheManager): Unit = {
        //return
        AdaptiveSerializer.updateCache(manager)
    }

    def blackListFromCachedSerializer(target: String): Unit = {
        AdaptiveSerializer.blackListFromCachedSerializer(target)
    }

    private object AdaptiveSerializer {

        val rawSerializer: RawObjectSerializer.type = RawObjectSerializer
        @Nullable
        @volatile private var cachedSerializer         : CachedObjectSerializer   = _ //Will be instantiated once connection with the server is handled.
        @Nullable
        @volatile private var cachedSerializerWhitelist: SharedCollection[String] = _

        def serialize(info: TransferInfo): PacketSerializationResult = {
            //Thread.dumpStack()
            lazy val serializer = if (initialised) {
                val whiteListArray = cachedSerializerWhitelist.toArray
                info.coords.determineSerializer(whiteListArray, rawSerializer, cachedSerializer)
            } else {
                rawSerializer
            }
            try {
                //AppLogger.debug(s"${currentTasksId} <> Serializing $packet, $coordinates with serializer ${serializer.getClass.getSimpleName}")
                LazyPacketSerializationResult(info, () => serializer)
            } catch {
                case NonFatal(e) =>
                    throw PacketException(s"Could not serialize packet info '$info'", e)
            }
        }

        def deserialize(bytes: Array[Byte]): PacketTransferResult = {
            lazy val serializer = {
                if (bytes.startsWith(RawObjectSerializer.Signature))
                    rawSerializer
                else if (bytes.startsWith(CachedObjectSerializer.Signature))
                    cachedSerializer
                else throw new IllegalStateException(s"Received unknown packet signature. (${new String(bytes)})")
            }
            LazyPacketDeserializationResult(bytes, () => serializer)
        }

        def serialize(any: Serializable, target: String): Array[Byte] = {
            val serializer = if (initialised && cachedSerializerWhitelist.contains(target)) {
                cachedSerializer
            } else rawSerializer
            serializer.serialize(any, false)
        }

        def updateCache(cache: SharedCacheManager): Unit = {
            cachedSerializer = new CachedObjectSerializer(cache)
            rawSerializer.drainAllStrategies(cachedSerializer)

            if (cachedSerializerWhitelist == null)
                AppLogger.info(s"$ownerIdentifier: Stage 2 completed : Main cache manager created.")

            cachedSerializerWhitelist = cache.getCache(15, SharedCollection[String])
            cachedSerializerWhitelist.add(ownerIdentifier)
            cachedSerializerWhitelist.addListener((_, _, _) => AppLogger.warn(s"Whitelist : $cachedSerializerWhitelist"))
        }

        def attachStrategy(strategy: SerialStrategy[_]): Unit = {
            rawSerializer.attachStrategy(strategy)
            if (initialised)
                cachedSerializer.attachStrategy(strategy)
        }

        def initialised: Boolean = cachedSerializerWhitelist != null

        def blackListFromCachedSerializer(target: String): Unit = {
            if (cachedSerializerWhitelist != null)
                cachedSerializerWhitelist.remove(target)
        }
    }

}
