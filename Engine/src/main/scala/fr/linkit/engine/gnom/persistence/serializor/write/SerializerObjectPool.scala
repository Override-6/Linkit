/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.persistence.serializor.write

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.obj.{InstanceObject, ReferencedNetworkObject}
import fr.linkit.api.gnom.reference.NetworkReferenceLocation
import fr.linkit.engine.gnom.persistence.pool.{ObjectPool, PoolChunk, SimpleReferencedNetworkObject}
import fr.linkit.engine.gnom.persistence.serializor.ArrayPersistence
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.internal.utils.UnWrapper

class SerializerObjectPool(bundle: PersistenceBundle, sizes: Array[Int]) extends ObjectPool(sizes) {

    private         val config          = bundle.config
    private         val coordinates     = bundle.coordinates
    private         val gnol            = bundle.gnol
    protected final val chunksPositions = new Array[Int](chunks.length)

    def getChunk[T](ref: Any): PoolChunk[T] = {
        //TODO this method can be optimized
        ref match {
            case c: Class[_] if isSyncClass(c) => getChunkFromFlag(SyncClass)
            case _: Class[_]                   => getChunkFromFlag(Class)

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

    override def freeze(): Unit = {
        super.freeze()
        var i            = 0
        val len          = chunksPositions.length
        var currentIndex = 0
        while (i < len) {
            val chunkSize = chunks(i).size
            if (chunkSize == 0)
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
    private def isSyncClass(clazz: Class[_]): Boolean = {
        classOf[SynchronizedObject[_]].isAssignableFrom(clazz)
    }

    @inline
    private def globalPos(ref: Any): Int = {
        if (ref == null)
            return 0
        val chunk = getChunk[Any](ref)
        val tag   = chunk.tag
        var idx   = chunk.indexOf(ref)
        if (idx > -1)
            idx += chunksPositions(chunk.tag) + 1
        else if (tag == Object) { //it may be a referenced object
            idx = chunks(RNO).indexOf(ref) + chunksPositions(RNO) + 1
        }
        idx
    }

    def addObject(ref: AnyRef): Unit = {
        if (isFrozen)
            throw new IllegalStateException("Could not add object: This pool is frozen !")
        addObj(ref)
    }

    private def addArray(array: AnyRef): Unit = {
        val comp = array.getClass.componentType()
        getChunkFromFlag(Array).add(array)
        if (!comp.isPrimitive) {
            val a = array.asInstanceOf[Array[Any]] //it's an array of object
            addObj(ArrayPersistence.getAbsoluteCompType(a)._1)
            addAll(a)
        }
    }

    private def addObj(ref: AnyRef): Unit = {
        //just do not add null elements (automatically referenced to '0' when written)
        if ((ref eq null) || getChunk(ref).indexOf(ref) > 0)
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
            case _ if UnWrapper.isPrimitiveWrapper(ref) =>
                getChunk(ref).add(ref)
            case _                                      =>
                addObj0(ref)
        }
    }

    private def addObj0(ref: AnyRef): Unit = {
        val profile = config.getProfile[AnyRef](ref.getClass)
        val nrlOpt  = gnol.findObjectLocation[NetworkReferenceLocation](coordinates, ref)
        if (nrlOpt.isEmpty) {
            addTypeOfIfAbsent(ref)
            val persistence = profile.getPersistence(ref)
            val decomposed  = persistence.toArray(ref)
            val objPool     = getChunkFromFlag[InstanceObject[AnyRef]](Object)
            objPool.add(new PacketObject(ref, decomposed, profile))
            addAll(decomposed)
        } else {
            val pool = getChunkFromFlag[ReferencedNetworkObject](RNO)
            val nrl  = nrlOpt.get
            addObj(nrl)
            val rno = new SimpleReferencedNetworkObject(globalPos(nrl), nrl, ref)
            pool.add(rno)
        }
    }

    private def addTypeOfIfAbsent(ref: AnyRef): Unit = ref match {
        case sync: SynchronizedObject[_] => getChunkFromFlag(SyncClass).addIfAbsent(sync.getSuperClass)
        case _                           => getChunkFromFlag(Class).addIfAbsent(ref.getClass)
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
