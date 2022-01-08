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

package fr.linkit.api.gnom.cache.sync.contract.descriptors

import fr.linkit.api.gnom.cache.sync.contract.StructureContractDescriptor
import fr.linkit.api.gnom.cache.sync.contract.behavior.SyncObjectContext
import fr.linkit.api.gnom.cache.sync.contractv2.{ObjectStructureContract, StaticsStructureContract}

trait StructureBehaviorDescriptorNode[A <: AnyRef] {

    val descriptor: StructureContractDescriptor[A]

    def foreachNodes(f: StructureBehaviorDescriptorNode[_ >: A] => Unit): Unit

    def getStaticContract(clazz: Class[_], context: SyncObjectContext): StaticsStructureContract[A]

    def getObjectContract(clazz: Class[_], context: SyncObjectContext): ObjectStructureContract[A]
}
