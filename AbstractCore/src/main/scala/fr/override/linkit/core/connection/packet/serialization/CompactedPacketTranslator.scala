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

package fr.`override`.linkit.core.connection.packet.serialization

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.packet._
import fr.`override`.linkit.api.connection.packet.serialization.{PacketDeserializationResult, PacketSerializationResult, PacketTranslator, Serializer}
import fr.`override`.linkit.api.local.system.security.BytesHasher
import fr.`override`.linkit.core.connection.network.cache.collection.SharedCollection
import fr.`override`.linkit.core.connection.packet.serialization.CachedObjectSerializer
import fr.`override`.linkit.core.local.system.ContextLogger
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class CompactedPacketTranslator(ownerIdentifier: String, securityManager: BytesHasher) extends PacketTranslator { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel
    override def translate(packet: Packet, coordinates: PacketCoordinates): PacketSerializationResult = {
        val result = SmartSerializer.serialize(packet, coordinates)
        securityManager.hashBytes(result.bytes)
        result
    }

    override def translate(bytes: Array[Byte]): PacketDeserializationResult = {
        securityManager.deHashBytes(bytes)
        SmartSerializer.deserialize(bytes)
    }

    override val signature: Array[Byte] = new Array(3)

    def updateCache(manager: SharedCacheManager): Unit = {
        //return
        SmartSerializer.updateCache(manager)
    }

    def blackListFromCachedSerializer(target: String): Unit = {
        SmartSerializer.blackListFromCachedSerializer(target)
    }

    private object SmartSerializer {
        private val rawSerializer = RawObjectSerializer
        @Nullable
        @volatile private var cachedSerializer: ObjectSerializer = _ //Will be instantiated once connection with the server is handled.
        @Nullable
        @volatile private var cachedSerializerWhitelist: SharedCollection[String] = _

        def serialize(packet: Packet, coordinates: PacketCoordinates): PacketSerializationResult = {
            //Thread.dumpStack()
             def serializer(): Serializer = if (initialised) {
                val whiteListArray = cachedSerializerWhitelist.toArray
                coordinates.determineSerializer(whiteListArray, rawSerializer, cachedSerializer)
            } else {
                rawSerializer
            }
            try {
                //ContextLogger.debug(s"Serializing $packet, $coordinates with serializer ${serializer.getClass.getSimpleName}")
                PacketSerializationResult(packet, coordinates, serializer)
            } catch {
                case NonFatal(e) =>
                    throw PacketException(s"Could not serialize packet and coordinates $packet, $coordinates.", e)
            }
        }

        def deserialize(bytes: Array[Byte]): PacketDeserializationResult = {
            def serializer(): Serializer = if (rawSerializer.isSameSignature(bytes)) {
                rawSerializer
            } else if (initialised && cachedSerializer.isSameSignature(bytes)) {
                cachedSerializer
            } else {
                throw new IllegalStateException("Received unknown serializer signature. (or maybe cached serializer, but this translator is unable to match it while it stands not initialized.)")
            }
            PacketDeserializationResult(serializer, bytes)
        }

        def updateCache(cache: SharedCacheManager): Unit = {
            cachedSerializer = new CachedObjectSerializer(cache)
            cachedSerializerWhitelist = cache.get(15, SharedCollection[String])
            cachedSerializerWhitelist.add(ownerIdentifier)
            //cachedSerializerWhitelist.addListener((_, _, _) => ContextLogger.debug(s"Whitelist : $cachedSerializerWhitelist"))
        }

        def initialised: Boolean = cachedSerializerWhitelist != null

        def blackListFromCachedSerializer(target: String): Unit = {
            if (cachedSerializerWhitelist != null)
                cachedSerializerWhitelist.remove(target)
        }
    }

}
