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
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.persistence.obj.{SyncPoolObject, PoolObject}
import fr.linkit.api.gnom.reference.NetworkObjectReference

class MirroredObjectWrite(override val stubClassDef: SyncClassDef,
                          override val referenceIdx: Int,
                          override val reference: NetworkObjectReference,
                          override val value: ChippedObject[_]) extends SyncPoolObject {
    override def equals(obj: Any): Boolean = {
        obj match {
            case ref: PoolObject[_] => ref.value == value.connected
            case ref: AnyRef        => ref == value.connected
        }
    }

    override val identity: Int = System.identityHashCode(value.connected)

}
