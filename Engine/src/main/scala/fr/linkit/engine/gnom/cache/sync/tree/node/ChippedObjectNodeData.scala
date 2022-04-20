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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.network.Network
import org.jetbrains.annotations.Nullable

import java.lang.ref.WeakReference

class ChippedObjectNodeData[A <: AnyRef](val network: Network, //Remote invocations
                                         val chip: Chip[A], //Reflective invocations
                                         val contract: StructureContract[A],
                                         @Nullable val origin: WeakReference[AnyRef]) //The synchronized object's origin (the same object before it was converted to its synchronized version, if any).
                                        (data: NodeData[A]) extends NodeData[A](data) {

    def this(other: ChippedObjectNodeData[A]) = {
        this(other.network, other.chip, other.contract, other.origin)(other.data)
    }

}