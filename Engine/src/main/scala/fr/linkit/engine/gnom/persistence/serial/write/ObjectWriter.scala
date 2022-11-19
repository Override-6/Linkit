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

package fr.linkit.engine.gnom.persistence.serial.write

import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefMultiple, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.persistence.context.{Decomposition, ObjectTranform, Replaced}
import fr.linkit.api.gnom.persistence.obj.ReferencedPoolObject
import fr.linkit.api.gnom.persistence.{Freezable, PersistenceBundle}
import fr.linkit.engine.gnom.network.EngineImpl
import fr.linkit.engine.gnom.persistence.obj.PoolChunk
import fr.linkit.engine.gnom.persistence.ProtocolConstants._
import fr.linkit.engine.gnom.persistence.serial.{ArrayPersistence, PacketPoolTooLongException}
import fr.linkit.engine.internal.mapping.ClassMappings

import java.nio.ByteBuffer
import java.util.ConcurrentModificationException
import scala.annotation.switch

class ObjectWriter(bundle: PersistenceBundle) extends Freezable {

    val buff: ByteBuffer = bundle.buff
    private final val boundClassMappings = bundle.network.getEngine(bundle.boundNT).flatMap(_.asInstanceOf[EngineImpl].classMappings).orNull
    //private       val config             = bundle.config
    private var packetRefSize            = 1
    private       val pool               = new SerializerObjectPool(bundle)

    def addObjects(roots: Array[AnyRef]): Unit = {
        val pool = this.pool
        //Registering all objects, and contained objects in the constant pool.
        for (o <- roots) {
            pool.addObject(o)
        }
    }

    override def freeze(): Unit = pool.freeze()

    override def isFrozen: Boolean = pool.isFrozen

    /**
     * Will writes the chunks contained in the pool
     * This is a terminal action:
     */
    def writePool(): Unit = {
        if (pool.isFrozen)
            throw new IllegalStateException("Pool is frozen.")
        pool.freeze() //Ensure that the pool is no more susceptible to be updated.

        val poolSize = pool.size
        packetRefSize = {
            if (poolSize > (java.lang.Short.MAX_VALUE * 2 + 1)) IntSize
            else if (poolSize > java.lang.Byte.MAX_VALUE * 2 + 1) UShortSize
                 else UByteSize
        }

        //Informs the deserializer if we send a wide packet or not.
        //This means that for wide packets, pool references index are ints, and
        //for "non wide packets", references index are unsigned shorts
        buff.put(packetRefSize.toByte)
        //Write the content
        writeChunks()
    }

    @inline
    private def writeChunks(): Unit = {
        //println(s"Write chunks : ${buff.array().mkString(", ")}")
        //let a hole for placing in chunk sizes

        val chunks    = pool.getChunks
        var totalSize = 0

        val announcedChunksPos = buff.position()
        buff.position(announcedChunksPos + 8)

        //Announcing what chunk has been used by the packet and writing their sizes in the buff
        var announcedChunksNumber: Long = 0 //0b00000000

        var i = 0
        while (i < ChunkCount) {
            val chunk = chunks(i)
            val size  = chunk.size
            if (size > 0) {
                totalSize += size
                //Tag's announcement mark is appended to the announced chunks number
                announcedChunksNumber |= (1 << chunk.tag)

                putRef(size) //append the size of the chunk
            }
            i += 1
        }
        buff.putLong(announcedChunksPos, announcedChunksNumber)
        i = 0
        while (i < ChunkCount) {
            val chunk = chunks(i)
            val size  = chunk.size
            if (size > 0) {
                writeChunk(i.toByte, chunk)
            }
            i += 1
        }
        //just here to check if the total pool size is not larger than Char.MaxValue if (widePacket == false)
        //or if the total size is not larger than Int.MaxValue (if widePacket == true)
        //else throw PacketPoolTooLongException.
        if (totalSize > scala.Int.MaxValue)
            throw new PacketPoolTooLongException(s"Packet total items size exceeded integer maximum value (total size: $totalSize)")
    }

    private def writeChunk(flag: Byte, chunk: PoolChunk[Any]): Unit = {
        val size = chunk.size
        //Write content
        if (flag >= Int && flag < Char) {
            ArrayPersistence.writePrimitiveArrayContent(this, chunk.array, flag, 0, size)
            return
        }

        @inline
        def foreach[T](@inline action: T => Unit): Unit = {
            val items = chunk.array.asInstanceOf[Array[T]]
            var i     = 0
            while (i < size) {
                val item = items(i)
                //println(s"Writing item $item (pos: ${buff.position()})")
                action(item)
                //println(s"Item Written! (pos: ${buff.position()})")
                i += 1
            }
        }

        (flag: @switch) match {
            case Class   => foreach[Class[_]](writeClass)
            case SyncDef => foreach[SyncClassDef](writeSyncClassDef)
            case String  => foreach[String](writeString)
            case Enum    => foreach[Enum[_]](writeEnum)
            case Array   => foreach[AnyRef](xs => ArrayPersistence.writeArray(this, xs))
            case Object  => foreach[SimpleObject](writeObject)
            case RNO     => foreach[ReferencedPoolObject](obj => putRef(obj.referenceIdx))
        }
    }

    def getPool: SerializerObjectPool = pool

    private def writeSyncClassDef(syncDef: SyncClassDef): Unit = {
        syncDef.ensureOverrideable()
        val (classCount, classes) = syncDef match {
            case unique: SyncClassDefUnique     => (1, scala.Array(unique.mainClass))
            case multiple: SyncClassDefMultiple => (1 + multiple.interfaces.length, scala.Array(multiple.mainClass) ++ multiple.interfaces)
        }
        buff.putChar(classCount.toChar)
        classes.foreach(putTypeRef)
    }

    private def writeClass(clazz: Class[_]): Unit = {
        val code = ClassMappings.codeOfClass(clazz)
        buff.putInt(code)
        if (boundClassMappings != null && !boundClassMappings.isClassCodeMapped(code)) InvocationChoreographer.ensinv {
            boundClassMappings.addClassToMap(clazz.getName)
        }
    }

    private def writeEnum(enum: Enum[_]): Unit = {
        putTypeRef(enum.getClass)
        writeString(enum.name())
    }

    private def writeString(str: String): Unit = {
        val bytes = str.getBytes()
        buff.putInt(bytes.length).put(bytes)
    }

    private def writeObject(poolObj: SimpleObject): Unit = {
        poolObj.valueClassRef match {
            case Left(clazz)    =>
                putTypeRef(clazz)
            case Right(syncDef) =>
                putSyncTypeRef(syncDef)
        }
        poolObj.transform match {
            case Decomposition(decomposed) =>
                //writing object content in an array
                ArrayPersistence.writeArrayContent(this, decomposed)
            case Replaced(replacement)     =>
                //write array's length to -1 to specify that the object is located at another pool index
                buff.putInt(-1)
                putPoolRef(replacement)
        }
    }

    @inline
    def putRef(ref: Int): Unit = {
        if (ref < 0)
            throw new IndexOutOfBoundsException(s"Received negative reference index.")
        (packetRefSize: @switch) match {
            case UByteSize  => buff.put(ref.toByte)
            case UShortSize => buff.putChar(ref.toChar)
            case IntSize    => buff.putInt(ref)
        }
    }

    @inline
    def putPoolRef(obj: Any): Unit = {
        val idx = pool.globalPosition(obj)
        if (idx < 0) {
            pool.globalPosition(obj) //for debugger purposes
            throw new ConcurrentModificationException(s"Unknown object $obj, Maybe the object has been added or it has replaced an old object during the serialisation.")
        }
        putRef(idx)
    }

    def putTypeRef(tpe: Class[_]): Unit = {
        putRef(pool.getChunkFromFlag(Class).indexOf(tpe))
    }

    def putSyncTypeRef(syncClassDef: SyncClassDef): Unit = {
        val idx = pool.getChunkFromFlag(SyncDef).indexOf(syncClassDef)
        putRef(pool.getChunkFromFlag(Class).size + idx)
    }
}