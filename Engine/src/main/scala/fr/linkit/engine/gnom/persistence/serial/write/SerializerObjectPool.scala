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

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefMultiple}
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.context.{Decomposition, Replaced, TypeProfile}
import fr.linkit.api.gnom.persistence.obj.{ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.api.internal.util.Unwrapper
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.persistence.PersistenceException
import fr.linkit.engine.gnom.persistence.ProtocolConstants._
import fr.linkit.engine.gnom.persistence.obj.{ObjectPool, ObjectSelector, PoolChunk}
import fr.linkit.engine.gnom.persistence.serial.ArrayPersistence
import fr.linkit.engine.internal.util.JavaUtils

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class SerializerObjectPool(bundle: PersistenceBundle) extends ObjectPool(new Array[Int](ChunkCount).mapInPlace(_ => -1)) {

    private         val config          = bundle.config
    private         val selector        = new ObjectSelector(bundle)
    protected final val chunksPositions = new Array[Int](chunks.length)

    private val locks =  ListBuffer.empty[Int]

    def size: Int = {
        var s = 0
        for (chunk <- chunks) s += chunk.size
        s
    }

    def getChunk[T](ref: Any): PoolChunk[T] = {
        //TODO this method can be optimized
        ref match {
            case _: SyncClassDef => getChunkFromFlag(SyncDef)
            case _: Class[_]     => getChunkFromFlag(Class)

            case _: Int     => getChunkFromFlag(Int)
            case _: Byte    => getChunkFromFlag(Byte)
            case _: Short   => getChunkFromFlag(Short)
            case _: Long    => getChunkFromFlag(Long)
            case _: Double  => getChunkFromFlag(Double)
            case _: Float   => getChunkFromFlag(Float)
            case _: Boolean => getChunkFromFlag(Boolean)
            case _: Char    => getChunkFromFlag(Char)

            case _: String                 => getChunkFromFlag(String)
            case _ if ref.getClass.isArray => getChunkFromFlag(Array)
            case _: Enum[_]                => getChunkFromFlag(Enum)
            case _                         => getChunkFromFlag(Object)
        }
    }

    def getLocks: List[Int] = locks.toList

    override def freeze(): Unit = {
        super.freeze()
        var i            = 0
        val len          = chunksPositions.length
        var currentIndex = 0
        while (i < len) {
            val chunkSize = chunks(i).size
            if (chunkSize <= 0)
                chunksPositions(i) = -1
            else {
                chunksPositions(i) = currentIndex
                currentIndex += chunkSize
            }
            i += 1
        }
    }

    /**
     * Returns the global index of the reference object, or -1 of the object is not stored into this pool.
     *
     * @throws IllegalArgumentException if this pool is not frozen.
     * */
    def globalPosition(ref: Any): Int = {
        if (!isFrozen)
            throw new IllegalStateException("Could not get global Index of ref: This pool is not frozen !")
        globalPos(ref)
    }

    @inline
    private def globalPos(ref: Any): Int = {
        if (ref == null)
            return 0 //0 is the index for nulls
        val chunk = getChunk[Any](ref)
        var tag   = chunk.tag
        var idx   = chunk.indexOf(ref)
        if (idx > -1)
            return idx + chunksPositions(tag) + 1

        tag = RNO
        idx = chunks(RNO).indexOf(ref)
        if (idx < 0)
            return -1
        idx + chunksPositions(tag) + 1
    }

    def addObject(ref: AnyRef): Unit = {
        if (isFrozen)
            throw new IllegalStateException("Could not add object: This pool is frozen !")
        addObj(ref)
    }

    private def addArray(array: AnyRef): Unit = {
        val compType = array.getClass.componentType()
        getChunkFromFlag(Array).add(array)
        if (!compType.isPrimitive) {
            val a            = array.asInstanceOf[Array[Any]] //it's an array of object
            val absoluteComp = ArrayPersistence.getAbsoluteCompType(a)._1
            addObj(absoluteComp)
            addAll(a)
        }
    }

    private def addObj(ref: AnyRef): Unit = {
        //just do not add null elements (automatically referenced to '0' when written)
        //nor do not add elements that are already contained in the pool
        if ((ref eq null) || getChunk(ref).indexOf(ref) >= 0)
            return
        ref match {
            case _: String                              =>
                getChunkFromFlag(String).add(ref)
            case _: AnyRef if ref.getClass.isArray      =>
                addArray(ref)
            case _: Enum[_]                             =>
                addTypeOfIfAbsent(ref)
                getChunkFromFlag(Enum).add(ref)
            case _: Class[_]                            =>
                getChunkFromFlag(Class).add(ref)
            case _ if Unwrapper.isPrimitiveWrapper(ref) =>
                getChunk(ref).add(ref)
            case _                                      =>
                addObj0(ref)
        }
    }

    private def addObj0(ref: AnyRef): Unit = {
        val nrlOpt = selector.findObjectReference(ref)
        if (nrlOpt.isEmpty) {
            ref match {
                case no: NetworkObject[_] =>
                    val refHash = no.reference.hashCode()
                    locks += refHash
                    AppLoggers.Persistence.trace(s"Writing Network object (referenceHash: $refHash) to ${bundle.boundNT}.")
                case _                    =>
            }
            addObjectDecomposed(ref)
        } else {
            addReferencedObject(ref, nrlOpt.get)
        }
    }

    private def addObjectDecomposed(ref: AnyRef): Unit = {
        val profile = config.getProfile[AnyRef](ref)
        addObjectDecomposed(ref, profile, 0)
    }

    private def addObjectDecomposed(ref: AnyRef, profile: TypeProfile[AnyRef], serializerFails: Int): Unit = {

        def retry(): Unit = {
            if (serializerFails >= profile.getPersistences.length) {
                throw new PersistenceException(s"Could not find a serializer that could successfully decompose $ref")
            }
            addObjectDecomposed(ref, profile, serializerFails + 1)
        }


        val persistence = profile.selectPersistor(ref, serializerFails)

        val transformation = try persistence.transform(ref) catch {
            case NonFatal(e) =>
                AppLoggers.Persistence.error(s"Persistor error when serializing object of type ${ref.getClass.getName}: " + e)
                retry()
                return
        }


        var selectedType: Either[Class[_], SyncClassDef] = null
        val subObjects  : Array[Any]                     = transformation match {
            case Decomposition(decomposed) =>
                val decomposedLength = decomposed.length
                if (decomposedLength == 1 && JavaUtils.sameInstance(decomposed(0), ref)) {
                    retry()
                    return
                }

                selectedType = addTypeOfIfAbsent(ref)
                decomposed
            case Replaced(replacement)     =>
                if (JavaUtils.sameInstance(ref, replacement)) {
                    retry()
                    return
                }
                selectedType = addTypeOfIfAbsent(replacement)
                scala.Array(replacement)
        }

        val poolObj = new SimpleObject(ref, selectedType, transformation)

        val objPool = getChunkFromFlag[ProfilePoolObject[AnyRef]](Object)
        objPool.add(poolObj)
        //v^ do not swap those two lines
        addAll(subObjects)
    }

    private def addReferencedObject(ref: AnyRef, nrl: NetworkObjectReference): Unit = {
        ref match {
            case no: NetworkObject[_] =>
                AppLoggers.Persistence.trace(s"Writing Network object (reference: ${no.reference}) to ${bundle.boundNT}. The object is replaced by its reference.")
            case _                    =>
        }

        val chunk = getChunkFromFlag[ReferencedPoolObject](RNO)
        if (chunk.indexOf(ref) >= 0)
            return
        val pos = chunks(Object).size
        addObj(nrl)
        val rno: ReferencedPoolObject = new ReferencedPoolObject {
            override val referenceIdx: Int                    = pos
            override val reference   : NetworkObjectReference = nrl

            override def value: AnyRef = ref
        }
        chunk.add(rno)
    }

    private def addTypeOfChippedIfAbsent(chi: ChippedObject[_]): SyncClassDef = {
        val implClassDef = chi.getNode.contract.mirroringInfo.fold[SyncClassDef](chi.getClassDef)(_.stubSyncClass)

        val idx = getChunkFromFlag(SyncDef).addIfAbsent(implClassDef)
        if (idx < 0) {
            val classChunk = getChunkFromFlag[Class[_]](Class)
            classChunk.add(implClassDef.mainClass)
            implClassDef match {
                case multiple: SyncClassDefMultiple =>
                    multiple.interfaces.foreach(classChunk.add)
                case _                              =>
            }
        }
        implClassDef
    }

    private def addTypeOfIfAbsent(ref: AnyRef): Either[Class[_], SyncClassDef] = ref match {
        case chi: ChippedObject[_] =>
            Right(addTypeOfChippedIfAbsent(chi))
        case _                     =>
            ChippedObjectAdapter.findAdapter(ref) match {
                case Some(chi) =>
                    Right(addTypeOfChippedIfAbsent(chi))
                case None      =>
                    val classChunk = getChunkFromFlag[Class[_]](Class)
                    val clazz      = ref.getClass
                    classChunk.addIfAbsent(clazz)
                    Left(clazz)
            }
    }

    private def addAll(objects: Array[Any]): Unit = {
        var i   = 0
        val len = objects.length
        while (i < len) {
            addObj(objects(i).asInstanceOf[AnyRef])
            i += 1
        }
    }
}
