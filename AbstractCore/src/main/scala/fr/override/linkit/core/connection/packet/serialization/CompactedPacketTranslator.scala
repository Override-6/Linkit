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

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.packet._
import fr.`override`.linkit.api.connection.packet.serialization.{PacketSerializationResult, PacketTranslator}
import fr.`override`.linkit.api.local.system.security.BytesHasher
import fr.`override`.linkit.core.connection.network.cache.collection.SharedCollection
import fr.`override`.linkit.core.connection.packet.serialization.CachedObjectSerializer
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class CompactedPacketTranslator(ownerIdentifier: String, securityManager: BytesHasher) extends PacketTranslator { //Notifier is accessible from api to reduce parameter number in (A)SyncPacketChannel

    override def translate(bytes: Array[Byte]): (Packet, PacketCoordinates) = {
        securityManager.deHashBytes(bytes)
        SmartSerializer.deserialize(bytes).swap
    }

    override def translate(packet: Packet, coordinates: PacketCoordinates): PacketSerializationResult = {
        val result = SmartSerializer.serialize(packet, coordinates)
        securityManager.hashBytes(result.bytes)
        result
    }

    override val signature: Array[Byte] = new Array(3)

    def update(connection: ConnectionContext): Unit = {
        return
        SmartSerializer.completeInitialisation(connection.network.globalCache)
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
            val serializer = if (initialised) {
                val whiteListArray = cachedSerializerWhitelist.toArray
                coordinates.determineSerializer(whiteListArray, rawSerializer, cachedSerializer)
            } else {
                rawSerializer
            }
            try {
                //println(s"Serializing $packet, $coordinates in thread ${Thread.currentThread()} with serializer ${serializer.getClass.getSimpleName}")
                val bytes = serializer.serialize(Array(coordinates, packet))
                PacketSerializationResult(packet, coordinates, serializer, bytes)
            } catch {
                case NonFatal(e) =>
                    throw PacketException(s"Could not serialize packet and coordinates $packet, $coordinates.", e)
            }
        }

        def deserialize(bytes: Array[Byte]): (PacketCoordinates, Packet) = {
            val serializer = if (rawSerializer.isSameSignature(bytes)) {
                rawSerializer
            } else if (!initialised) {
                throw new IllegalStateException("Received cached serialisation signature but this packet translator is not ready to handle it.")
            } else {
                cachedSerializer
            }
            val array = try {
                serializer.deserializeAll(bytes)
            } catch {
                case NonFatal(e) =>
                    throw PacketException(s"Could not deserialize bytes ${new String(bytes)} to packet.", e)

            }
            //println(s"Deserialized ${array.mkString("Array(", ", ", ")")}")
            (array(0).asInstanceOf[PacketCoordinates], array(1).asInstanceOf[Packet])
        }

        def completeInitialisation(cache: SharedCacheManager): Unit = {
            if (cachedSerializer != null)
                throw new IllegalStateException("This packet translator is already fully initialised !")

            cachedSerializer = new CachedObjectSerializer(cache)
            cachedSerializerWhitelist = cache.get(15, SharedCollection[String])
            cachedSerializerWhitelist.add(ownerIdentifier)
        }

        def initialised: Boolean = cachedSerializerWhitelist != null

        def blackListFromCachedSerializer(target: String): Unit = {
            if (cachedSerializerWhitelist != null)
                cachedSerializerWhitelist.remove(target)
        }
    }

}
