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

package fr.linkit.engine.gnom.persistence.serial.read

import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.ControlBox
import fr.linkit.api.gnom.persistence.obj.{PoolObject, ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.persistence.MalFormedPacketException
import fr.linkit.engine.gnom.persistence.ProtocolConstants._
import fr.linkit.engine.gnom.persistence.config.SimpleControlBox
import fr.linkit.engine.gnom.persistence.obj.{NetworkObjectReferencesLocks, ObjectSelector}
import fr.linkit.engine.gnom.persistence.serial.{ArrayPersistence, ClassNotMappedException, ObjectDeserializationException}
import fr.linkit.engine.internal.mapping.ClassMappings

import java.lang.reflect.{Array => RArray}
import java.nio.ByteBuffer
import java.util.concurrent.locks.Lock
import scala.annotation.switch
import scala.reflect.ClassTag

class PacketReader(bundle         : PersistenceBundle,
                   syncClassCenter: SyncClassCenter) {

    final         val buff   : ByteBuffer             = bundle.buff
    private final val selector                        = new ObjectSelector(bundle)
    private lazy  val boundMappings                   = bundle.network.getEngine(bundle.boundNT).flatMap(_.asInstanceOf[EngineImpl].classMappings).orNull
    private final val config                          = bundle.config
    private var packetRefSize: Int                    = -1
    private var pool         : DeserializerObjectPool = _
    private var isInit                                = false
    val controlBox: ControlBox = new SimpleControlBox()

    def read(readExecution: => Unit): Unit = {
        if (isInit)
            throw new IllegalStateException("This reader is already initialised.")
        isInit = true
        val locks = readLocksAndAcquire()
        readPool()
        readExecution
        locks.foreach(_.unlock())
    }

    private def readLocksAndAcquire(): List[Lock] = {
        var locks: List[Lock] = Nil
        val len               = buff.getInt
        var i                 = 0
        while (i < len) {
            val lockCode = buff.getInt
            val lock = NetworkObjectReferencesLocks.getComputationLock(lockCode)
            lock.lock()
            locks ::= lock
            i += 1
        }
        locks
    }

    private def readPool(): Unit = {
        readPoolStructure()
        val sizes = pool.sizes
        var i: Byte = 0
        //println(s"Read chunks : ${buff.array().mkString(", ")}")
        while (i < ChunkCount) {
            val size = sizes(i)
            if (size > 0) {
                //println(s"Read chunk. pos of ${i} = ${buff.position()}")
                readNextChunk(size, i)
                //println(s"End Read chunk. end pos of ${i} = ${buff.position()}")
            }
            i = (i + 1).toByte
        }
    }

    def getPool: DeserializerObjectPool = pool

    @inline
    def readNextRef: Int = (packetRefSize: @switch) match {
        case UByteSize  => buff.get
        case UShortSize => buff.getChar
        case IntSize    => buff.getInt
    }

    private def readNextChunk(size: Int, flag: Byte): Unit = {
        if (flag >= Int && flag <= Char) {
            val chunk   = pool.getChunkFromFlag[Any](flag)
            val content = ArrayPersistence.readPrimitiveArray(size, flag, this)
            System.arraycopy(content, 0, chunk.array, 0, RArray.getLength(content))
            return
        }

        @inline
        def collectAndUpdateChunk[T: ClassTag](@inline action: => T): Unit = {
            var i     = 0
            val chunk = pool.getChunkFromFlag[Any](flag)
            chunk.resetPos()
            while (i < size) {
                //println(s"reading item (type: $flag, pos: ${buff.position()})")
                val item: T = action
                //println(s"Item read ! (type: $flag, pos: ${buff.position()})")
                chunk.add(item)
                i += 1
            }
        }

        (flag: @switch) match {
            case Class   => collectAndUpdateChunk[Class[_]](readClass())
            case SyncDef => collectAndUpdateChunk[SyncClassDef](readSyncDef())
            case Enum    => collectAndUpdateChunk[Enum[_]](readEnum())
            case String  => collectAndUpdateChunk[String](readString())
            case Array   => collectAndUpdateChunk[PoolObject[_ <: AnyRef]](ArrayPersistence.readArray(this))
            case Object  => collectAndUpdateChunk[ProfilePoolObject[_]](readObject())
            case RNO     => collectAndUpdateChunk[ReferencedPoolObject](readReferencedObject())
        }
    }

    private def readReferencedObject(): ReferencedPoolObject = {
        new ReferencedObject(readNextRef, selector, pool)
    }

    private def readSyncDef(): SyncClassDef = {
        def readDef(): SyncClassDef = {
            val classCount = buff.getChar.toInt
            if (classCount == 1) {
                return SyncClassDef(pool.getType(readNextRef))
            }
            if (classCount < 1)
                throw new ObjectDeserializationException("class count < 1 when reading sync class definition.")
            val classes = new Array[Class[_]](classCount)
            var i       = 0
            while (i < classCount) {
                classes(i) = pool.getType(readNextRef)
                i += 1
            }
            SyncClassDef(classes.head, classes.tail)
        }

        val result = readDef()
        val clazz  = syncClassCenter.getSyncClass(result)
        pool.cacheSyncClass(result, clazz)
        result
    }

    private def readClass(): Class[_] = {
        val code  = buff.getInt
        val clazz = ClassMappings.getClass(code)
        if (clazz == null) InvocationChoreographer.ensinv {
            val name = boundMappings.requestClassName(code)
            if (name != null) try {
                return ClassMappings.putClass(name)
            } catch {
                case _: ClassNotMappedException =>
                    AppLoggers.Persistence.warn(s"Could not map class '$name' received from packet '${bundle.packetID}': class is not present in classpath. Will now determine if this class name corresponds to a generated class.")
                    boundMappings.requestGenerationInstructions(name)
            }
            throw new ClassNotMappedException(s"No class is bound to code $code")
        }
        clazz
    }

    private def readEnum[T <: Enum[T]](): Enum[T] = {
        val tpe  = pool.getType(readNextRef)
        val name = readString()
        java.lang.Enum.valueOf[T](tpe.asInstanceOf[Class[T]], name)
    }

    private def readPoolStructure(): Unit = {
        packetRefSize = buff.get()
        if (packetRefSize > 3 || packetRefSize < 1) throw new MalFormedPacketException(s"packetRefSize is out of bounds: received $packetRefSize, expected: number between 1 and 3")
        val sizes = new Array[Int](ChunkCount)

        var i: Int                = 0
        val announcedChunksNumber = buff.getLong
        while (i < ChunkCount) {
            val chunkBit = (announcedChunksNumber >> i) & 1
            if (chunkBit == 1) {
                sizes(i) = readNextRef
            }
            i += 1
        }
        this.pool = new DeserializerObjectPool(sizes)
    }

    private def readObject(): ProfilePoolObject[AnyRef] = {
        val classRef = readNextRef
        val clazz    = pool.getType(classRef)
        val profile  = config.getProfile[AnyRef](clazz)
        val flag     = buff.getInt

        if (flag == ReplacementFlag) {
            val replacementIndex = readNextRef
            return new ReplacedObject(replacementIndex, pool)
        }

        //if flag is positive, it's the object's content length
        val content = readObjectContent(flag)
        new NotInstantiatedObject[AnyRef](profile, clazz, content, controlBox, selector, pool)
    }

    private def readObjectContent(length: Int): Array[Int] = {
        var i       = 0
        val content = new Array[Int](length)
        while (i < length) {
            content(i) = readNextRef
            i += 1
        }
        content
    }

    private def readString(): String = {
        val size  = buff.getInt()
        val array = new Array[Byte](size)
        buff.get(array)
        new String(array)
    }
}
