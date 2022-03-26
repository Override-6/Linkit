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

package fr.linkit.engine.gnom.persistence.serializor.write

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.obj.{InstanceObject, ReferencedNetworkObject}
import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.persistence.defaults.lambda.{NotSerializableLambdasTypePersistence, SerializableLambdasTypePersistence}
import fr.linkit.engine.gnom.persistence.obj.{ObjectPool, ObjectSelector, PoolChunk}
import fr.linkit.engine.gnom.persistence.serializor.ArrayPersistence
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.internal.utils.UnWrapper

class SerializerObjectPool(bundle: PersistenceBundle,
                           sizes: Array[Int]) extends ObjectPool(sizes) {

    private         val config          = bundle.config
    private         val selector        = new ObjectSelector(bundle)
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

            case _: String                          => getChunkFromFlag(String)
            case _ if ref.getClass.isArray          => getChunkFromFlag(Array)
            case _: Enum[_]                         => getChunkFromFlag(Enum)
            case ref: AnyRef if isLambdaObject(ref) => getChunkFromFlag(Lambda)
            case _                                  => getChunkFromFlag(Object)
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
        else
            if (tag == Object) { //it could be a referenced object
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
            val a            = array.asInstanceOf[Array[Any]] //it's an array of object
            val absoluteComp = ArrayPersistence.getAbsoluteCompType(a)._1
            addObj(absoluteComp)
            addAll(a)
        }
    }

    private def addLambda(lambdaObject: AnyRef): Unit = {
        val ltp            = lambdaObject match {
            case _: Serializable => SerializableLambdasTypePersistence
            case _               => NotSerializableLambdasTypePersistence
        }
        val representation = ltp.toRepresentation(lambdaObject)
        val decomposed     = addObjectAndReturnDecomposed(representation)
        val slo            = new SimpleLambdaObject(lambdaObject, decomposed, representation)
        getChunkFromFlag(Lambda).add(slo)
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
            case _ if UnWrapper.isPrimitiveWrapper(ref) =>
                getChunk(ref).add(ref)
            case _ if isLambdaObject(ref)               =>
                addLambda(ref)
            case _                                      =>
                addObj0(ref)
        }
    }

    @inline
    private def isLambdaObject(ref: AnyRef): Boolean = {
        val clazz = ref.getClass
        clazz.getSimpleName.contains("$$Lambda$")
    }

    private def addObjectAndReturnDecomposed(ref: AnyRef): Array[Any] = {
        val selectedRefType = addTypeOfIfAbsent(ref)
        val profile         = config.getProfile[AnyRef](ref)
        val persistence     = profile.getPersistence(ref)
        val decomposed      = persistence.toArray(ref)
        val objPool         = getChunkFromFlag[InstanceObject[AnyRef]](Object)

        //do not swap those two lines
        val obj = new SimpleObject(ref, selectedRefType, ref.isInstanceOf[SynchronizedObject[_]], decomposed, profile)
        objPool.add(obj)
        addAll(decomposed)
        decomposed
    }

    private def addObj0(ref: AnyRef): Unit = {
        //val clazz   = ref.getClass
        val nrlOpt = selector.findObjectReference(ref)
        if (nrlOpt.isEmpty) {
            addObjectAndReturnDecomposed(ref)
        } else {
            val chunk = getChunkFromFlag[ReferencedNetworkObject](RNO)
            if (chunk.indexOf(ref) >= 0)
                return
            val pos = chunks(Object).size
            val nrl = nrlOpt.get
            addObj(nrl)
            val rno: ReferencedNetworkObject = new ReferencedNetworkObject {
                override val referenceIdx         : Int                    = pos
                override val reference            : NetworkObjectReference = nrl
                override val value: AnyRef = ref
                override val identity: Int = System.identityHashCode(ref)
            }
            chunk.add(rno)
        }
    }

    private def addTypeOfIfAbsent(ref: AnyRef): Class[_] = ref match {
        case sync: SynchronizedObject[_] =>
            val implClass = sync.getNode.contract.remoteObjectInfo.fold[Class[_]](sync.getSourceClass)(_.stubClass)
            getChunkFromFlag(SyncClass).addIfAbsent(implClass)
            implClass
        case _                           =>
            val clazz = ref.getClass
            getChunkFromFlag(Class).addIfAbsent(clazz)
            clazz
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
