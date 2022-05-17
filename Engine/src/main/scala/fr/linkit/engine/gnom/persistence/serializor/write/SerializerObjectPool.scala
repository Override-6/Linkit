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

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefMultiple}
import fr.linkit.api.gnom.persistence.PersistenceBundle
import fr.linkit.api.gnom.persistence.obj.{ProfilePoolObject, ReferencedPoolObject}
import fr.linkit.api.gnom.reference.NetworkObjectReference
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter
import fr.linkit.engine.gnom.persistence.defaults.lambda.{NotSerializableLambdasTypePersistence, SerializableLambdasTypePersistence}
import fr.linkit.engine.gnom.persistence.obj.{ObjectPool, ObjectSelector, PoolChunk}
import fr.linkit.engine.gnom.persistence.serializor.ArrayPersistence
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol._
import fr.linkit.engine.internal.utils.UnWrapper
import org.jetbrains.annotations.Nullable

class SerializerObjectPool(bundle: PersistenceBundle) extends ObjectPool(new Array[Int](ChunkCount).mapInPlace(_ => -1)) {

    private         val config          = bundle.config
    private         val selector        = new ObjectSelector(bundle)
    protected final val chunksPositions = new Array[Int](chunks.length)

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

            case _: String                             => getChunkFromFlag(String)
            case _ if ref.getClass.isArray             => getChunkFromFlag(Array)
            case _: Enum[_]                            => getChunkFromFlag(Enum)
            case ref: AnyRef if isLambdaObject(ref)    => getChunkFromFlag(Lambda)
            case ch: ChippedObject[_] if ch.isMirrored => getChunkFromFlag(Mirroring)
            case _                                     => getChunkFromFlag(Object)
        }
    }

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
            return 0
        val chunk = getChunk[Any](ref)
        var tag   = chunk.tag
        var idx   = chunk.indexOf(ref)
        if (idx > -1)
            return idx + chunksPositions(tag) + 1

        tag = RNO
        idx = chunks(RNO).indexOf(ref)
        if (idx < 0) {
            tag = Mirroring
            idx = chunks(Mirroring).indexOf(ref)
        }
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
        val nrlOpt = selector.findObjectReference(lambdaObject)
        if (nrlOpt.nonEmpty) {
            addReferencedObject(lambdaObject, nrlOpt.get)
            return
        }

        val ltp            = lambdaObject match {
            case _: Serializable => SerializableLambdasTypePersistence
            case _               => NotSerializableLambdasTypePersistence
        }
        val representation = ltp.toRepresentation(lambdaObject)
        val decomposed     = addObjectDecomposed(representation)
        val slo            = new SimpleLambdaObject(lambdaObject, decomposed, representation)
        getChunkFromFlag(Lambda).add(slo)
    }

    private def addObj(ref: AnyRef): Unit = {
        //just do not add null elements (automatically referenced to '0' when written)
        //nor do not add elements that are already contained in the pool
        if ((ref eq null) || getChunk(ref).indexOf(ref) >= 0)
            return
        ref match {
            case _: String                               =>
                getChunkFromFlag(String).add(ref)
            case _: AnyRef if ref.getClass.isArray       =>
                addArray(ref)
            case _: Enum[_]                              =>
                addTypeOfIfAbsent(ref)
                getChunkFromFlag(Enum).add(ref)
            case _: Class[_]                             =>
                getChunkFromFlag(Class).add(ref)
            case _ if UnWrapper.isPrimitiveWrapper(ref)  =>
                getChunk(ref).add(ref)
            case _ if isLambdaObject(ref)                =>
                addLambda(ref)
            case chi: ChippedObject[_] if chi.isMirrored =>
                addMirroredObject(chi, null)
            case _                                       =>
                addObj0(ref)
        }
    }

    private def addMirroredObject(chi: ChippedObject[_], @Nullable refHint: Option[NetworkObjectReference]): Unit = {
        val nrlOpt = if (refHint == null) selector.findObjectReference(chi) else refHint
        if (nrlOpt.isEmpty) {
            addTypeOfIfAbsent(chi)
            val chunk        = getChunkFromFlag[ReferencedPoolObject](Mirroring)
            val pos          = chunks(Object).size
            val ref          = chi.reference
            val stubClassDef = chi.getNode.contract.remoteObjectInfo.get.stubSyncClass
            chunk.add(new MirroringObject(stubClassDef, pos, ref, chi))
            addObj(ref)
        } else {
            addReferencedObject(chi, nrlOpt.get)
        }
    }

    @inline
    private def isLambdaObject(ref: AnyRef): Boolean = {
        val clazz = ref.getClass
        clazz.getSimpleName.contains("$$Lambda$")
    }

    private def addObjectDecomposed(ref: AnyRef): Array[Any] = {
        val selectedRefType = addTypeOfIfAbsent(ref)
        val profile         = config.getProfile[AnyRef](ref)
        val persistence     = profile.getPersistence(ref)
        val decomposed      = persistence.toArray(ref)
        val objPool         = getChunkFromFlag[ProfilePoolObject[AnyRef]](Object)

        val obj = new SimpleObject(ref, selectedRefType, decomposed, profile)
        //do not swap those two lines
        objPool.add(obj)
        addAll(decomposed)
        decomposed
    }

    private def addObj0(ref: AnyRef): Unit = {
        val nrlOpt = selector.findObjectReference(ref)
        if (nrlOpt.isEmpty) {
            val chiOpt = ChippedObjectAdapter.findAdapter(ref)
            if (chiOpt.isDefined) addMirroredObject(chiOpt.get, nrlOpt)
            else addObjectDecomposed(ref)
        } else {
            addReferencedObject(ref, nrlOpt.get)
        }
    }

    private def addReferencedObject(ref: AnyRef, nrl: NetworkObjectReference): Unit = {
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

    private def addTypeOfIfAbsent(ref: AnyRef): Either[Class[_], SyncClassDef] = ref match {
        case sync: ChippedObject[_] =>
            val implClassDef = sync.getNode.contract.remoteObjectInfo.fold[SyncClassDef](sync.getClassDef)(_.stubSyncClass)

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
            Right(implClassDef)
        case _                      =>
            val classChunk = getChunkFromFlag[Class[_]](Class)
            val clazz      = ref.getClass
            classChunk.addIfAbsent(clazz)
            Left(clazz)
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
