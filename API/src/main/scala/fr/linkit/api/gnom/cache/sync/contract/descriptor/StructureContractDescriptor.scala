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

package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, SyncLevel}

sealed trait StructureContractDescriptor[A <: AnyRef] {

    val targetClass: Class[A]

    val methods: Array[MethodContractDescriptor]
    val fields : Array[FieldContract[Any]]
}

trait OverallStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A]

trait UniqueStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A] {

    val syncLevel: SyncLevel

}

trait MultiStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A] {

    val syncLevels: Set[SyncLevel]
}