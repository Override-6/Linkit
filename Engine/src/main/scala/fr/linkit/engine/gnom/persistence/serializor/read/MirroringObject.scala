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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.cache.NoSuchCacheException
import fr.linkit.api.gnom.cache.sync.contract.RegistrationKind
import fr.linkit.api.gnom.cache.sync.{ConnectedObjectReference, SynchronizedObjectCache}
import fr.linkit.api.gnom.persistence.obj.{MirroringPoolObject, ProfilePoolObject}
import fr.linkit.engine.gnom.cache.sync.tree.{DefaultSynchronizedObjectTree, NoSuchConnectedObjectTreeException}
import fr.linkit.engine.gnom.persistence.UnexpectedObjectException
import fr.linkit.engine.gnom.persistence.obj.ObjectSelector
import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol.Object

class MirroringObject(override val referenceIdx: Int,
                      override val stubClass: Class[_],
                      selector: ObjectSelector,
                      pool: DeserializerObjectPool) extends MirroringPoolObject {

    override lazy val reference: ConnectedObjectReference = pool
            .getChunkFromFlag[ProfilePoolObject[AnyRef]](Object)
            .get(referenceIdx)
            .value match {
        case l: ConnectedObjectReference => l
        case o                           =>
            throw new UnexpectedObjectException(s"Received reference '$o' to point at a mirrored connected object but it is not a connected object reference")
    }

    override val identity: Int = referenceIdx

    override lazy val value: AnyRef = {
        val ref      = reference
        val cacheRef = ref.parent.get
        lazy val errPrefix = "Could not deserialize mirroring object '$ref':"
        selector.findObject(cacheRef).getOrElse {
            throw new NoSuchCacheException(s"$errPrefix cache '$cacheRef' not found.")
        } match {
            case cache: SynchronizedObjectCache[_] =>
                val treeId    = ref.nodePath.head
                val tree      = cache.forest.findTree(ref.nodePath.head).getOrElse {
                    throw new NoSuchConnectedObjectTreeException(s"$errPrefix Could not find object tree $treeId in cache $ref")
                }.asInstanceOf[DefaultSynchronizedObjectTree[_]]
                val parentRef = new ConnectedObjectReference(ref.family, ref.cacheID, ref.ownerID, ref.nodePath.dropRight(1))
                val connected = tree.createConnectedObj(parentRef)(stubClass, RegistrationKind.Mirroring)
                connected
        }
    }
}