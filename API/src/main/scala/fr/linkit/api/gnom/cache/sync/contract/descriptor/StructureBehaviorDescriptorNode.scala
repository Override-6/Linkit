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

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.{ObjectContractFactory, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier

trait StructureBehaviorDescriptorNode[A <: AnyRef] {

    val descriptor: StructureContractDescriptor[A]

    def getStaticsContract(clazz: Class[_ <: A], context: SyncObjectContext): StructureContract[A]

    def getObjectContract(clazz: Class[_ <: A], context: SyncObjectContext, forceMirroring: Boolean): StructureContract[A]

    def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A]

}
