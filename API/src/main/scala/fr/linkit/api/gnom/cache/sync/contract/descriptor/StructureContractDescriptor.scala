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

package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, SyncLevel}

sealed trait StructureContractDescriptor[A <: AnyRef] {
    
    //enable auto chipping for objects expected as being synchronized / mirrored
    //but whose runtime class can't support generated 'Sync' class generation / overriding.
    val autochip: Boolean
    val targetClass: Class[A]

    val methods: Array[MethodContractDescriptor]
    val fields : Array[FieldContract[Any]]
}

trait OverallStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A]

trait UniqueStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A] {

    val syncLevel: SyncLevel

    override def toString: String = s"Unique $syncLevel (${targetClass.getName})"

}

trait MultiStructureContractDescriptor[A <: AnyRef] extends StructureContractDescriptor[A] {

    val syncLevels: Set[SyncLevel]

    override def toString: String = s"Multi (${syncLevels.mkString(", ")}) (${targetClass.getName})"

}