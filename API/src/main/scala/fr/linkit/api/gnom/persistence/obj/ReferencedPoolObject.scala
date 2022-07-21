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

package fr.linkit.api.gnom.persistence.obj

import fr.linkit.api.gnom.referencing.NetworkObjectReference

trait ReferencedPoolObject extends PoolObject[AnyRef] {

    /**
     * The [[NetworkObjectReference]] index in the chunk tag.
     * */
    val referenceIdx: Int
    val reference   : NetworkObjectReference

    override def equals(obj: Any): Boolean = {
        obj match {
            case ref: PoolObject[_] => ref.value == value
            case ref: AnyRef        => ref == value
        }
    }

    override val identity: Int = System.identityHashCode(value)
}
