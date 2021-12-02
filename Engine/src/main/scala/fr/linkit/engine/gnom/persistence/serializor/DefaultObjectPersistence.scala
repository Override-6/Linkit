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

package fr.linkit.engine.gnom.persistence.serializor

import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.persistence.obj.{PoolObject, RegistrablePoolObject}
import fr.linkit.api.gnom.persistence.{ObjectPersistence, PersistenceBundle}
import fr.linkit.api.internal.concurrency.WorkerPools.currentTasksId
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.persistence.serializor.read.ObjectReader
import fr.linkit.engine.gnom.persistence.serializor.write.{ObjectWriter, SerializerObjectPool}

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
        val t0 = System.currentTimeMillis()
        val buffer = bundle.buff
        buffer.put(signature.toArray)
        val writer = new ObjectWriter(bundle)
        writer.addObjects(objects)
        writer.writePool()
        val pool = writer.getPool
        writeEntries(objects, writer, pool)
        val t1 = System.currentTimeMillis()
        InvocationChoreographer.forceLocalInvocation {
            AppLogger.debug(s"Ended serialization of ${objects.mkString(", ")} (took ${t1 - t0} ms.)")
        }
    }

    private def writeEntries(objects: Array[AnyRef], writer: ObjectWriter,
                             pool: SerializerObjectPool): Unit = {
        //Write the size
        writer.putRef(objects.length)
        //Write the content

        for (o <- objects) {
            val idx = pool.globalPosition(o)
            writer.putRef(idx)
        }
    }

    override def deserializeObjects(bundle: PersistenceBundle)(forEachObjects: AnyRef => Unit): Unit = {
        val buff = bundle.buff
        checkSignature(buff)

        val reader = new ObjectReader(bundle, center)
        reader.readAndInit()
        val contentSize = buff.getChar
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


    private def checkSignature(buff: ByteBuffer): Unit = {
        if (!isSameSignature(buff))
            throw new IllegalArgumentException("Signature mismatches !")
        buff.position(buff.position() + signature.length)
    }

}
