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

package fr.linkit.engine.gnom.cache.sync.env.node

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.engine.gnom.cache.sync.env.SecondRegistryLayer

class ChippedObjectCompanionData[A <: AnyRef](val selector     : EngineSelector, //Remote invocations
                                              val chip         : Chip[A], //Reflective invocations
                                              val contract     : StructureContract[A],
                                              val choreographer: InvocationChoreographer,
                                              val secondLayer  : SecondRegistryLayer,
                                              chippedObject    : ChippedObject[A])
                                             (private val data: CompanionData[A]) extends CompanionData[A](data) {

    def this(other: ChippedObjectCompanionData[A]) = {
        this(other.selector, other.chip, other.contract, other.choreographer, other.secondLayer, other.obj)(other.data)
    }

    def obj: ChippedObject[A] = chippedObject

}