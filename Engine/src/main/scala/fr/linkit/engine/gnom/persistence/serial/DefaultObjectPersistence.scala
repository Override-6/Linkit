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

package fr.linkit.engine.gnom.persistence.serial

import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.persistence.obj.{PoolObject, RegistrablePoolObject}
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PersistenceBundle}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.persistence.ProtocolConstants
import fr.linkit.engine.gnom.persistence.serial.read.PacketReader
import fr.linkit.engine.gnom.persistence.serial.write.{PacketWriter, SerializerObjectPool}
import fr.linkit.engine.internal.concurrency.ComputationReleasableReentrantLock
import fr.linkit.engine.internal.debug.{Debugger, PacketDeserializationStep, PacketSerializationStep}

import java.nio.ByteBuffer

class DefaultObjectPersistence(center: SyncClassCenter) extends ObjectPersistence {

    override val signature: Seq[Byte] = Seq(12)

    override def isSameSignature(buffer: ByteBuffer): Boolean = {
        val pos    = buffer.position()
        val result = signature.forall(buffer.get.equals)
        buffer.position(pos)
        result
    }

    override def serializeObjects(objects: Array[AnyRef])(bundle: PersistenceBundle): Unit = {
        InvocationChoreographer.disinv {
            AppLoggers.Persistence.debug("Starting Serializing objects...")
            Debugger.push(PacketSerializationStep(bundle.packetID, bundle.boundNT))
            val t0     = System.currentTimeMillis()
            val buffer = bundle.buff
            try {
                buffer.put(signature.toArray)
                buffer.putShort(ProtocolConstants.ProtocolVersion)
                val writer = new PacketWriter(bundle)
                writer.addObjects(objects)
                buffer.limit(buffer.capacity())
                writer.writeAll()
                val pool = writer.getPool
                writeEntries(objects, writer, pool)
            } catch {
                case e: Throwable => throw e
            } finally {
                val t1 = System.currentTimeMillis()
                Debugger.pop()
                AppLoggers.Persistence.debug(s"Objects serialized (took ${t1 - t0} ms) - resulting buff length = ${buffer.position()}.")
            }
        }
    }

    private def writeEntries(objects: Array[AnyRef], writer: PacketWriter,
                             pool   : SerializerObjectPool): Unit = {
        //Write the number of root objects
        writer.putRef(objects.length)
        //Write the content
        for (o <- objects) {
            val idx = pool.globalPosition(o)
            writer.putRef(idx)
        }
    }

    override def deserializeObjects(bundle: PersistenceBundle)(forEachObjects: AnyRef => Unit): Unit = InvocationChoreographer.disinv {
        val buff = bundle.buff
        checkSignatureAndProtocol(buff)

        AppLoggers.Persistence.debug(s"Starting Deserializing objects... (from buff length: ${buff.limit()})")
        Debugger.push(PacketDeserializationStep(bundle.packetID, bundle.boundNT))

        try {
            val t0 = System.currentTimeMillis()

            val reader = new PacketReader(bundle, center)
            reader.read {
                val contentSize = reader.readNextRef
                val pool        = reader.getPool
                for (_ <- 0 until contentSize) {
                    val pos = reader.readNextRef
                    val obj = pool.getAny(pos) match {
                        case o: RegistrablePoolObject[AnyRef] =>
                            val value = o.value
                            o.register()
                            value
                        case o: PoolObject[AnyRef]            => o.value
                        case o: AnyRef                        => o
                    }
                    reader.controlBox.join()
                    forEachObjects(obj)
                }
            }
            val t1 = System.currentTimeMillis()
            AppLoggers.Persistence.debug(s"Objects deserialized (took ${t1 - t0} ms).")
        } catch {
            case e =>
                throw e //here to place a breakpoint if needed.
        } finally {
            Debugger.pop()
        }
    }

    private def checkSignatureAndProtocol(buff: ByteBuffer): Unit = {
        if (!signature.forall(buff.get.equals))
            throw new IllegalArgumentException("Signature mismatches !")
        val protocolVersion = buff.getShort
        if (protocolVersion != ProtocolConstants.ProtocolVersion)
            throw new IllegalArgumentException(s"Can't handle this packet: protocol signature mismatches ! (received: v$protocolVersion, can only read v${ProtocolConstants.ProtocolVersion}).")
    }

}
