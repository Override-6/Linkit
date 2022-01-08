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

package fr.linkit.engine.gnom.cache.sync.contract.behavior

import fr.linkit.api.gnom.cache.sync.contract.behavior.{ObjectContractFactory, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.contractv2.StructureContract

class SyncObjectContractFactory(override val data: ContractDescriptorData) extends ObjectContractFactory {


    override def getObjectContract[A <: AnyRef](clazz: Class[_], context: SyncObjectContext): StructureContract[A] = {
        data.getNode(clazz).getObjectContract(clazz, context: SyncObjectContext).asInstanceOf[StructureContract[A]]
    }

    override def getStaticContract[A <: AnyRef](clazz: Class[_]): StructureContract[A] = {
        ???
        ///data.getNode(clazz).getStaticContract()
    }
}

object SyncObjectContractFactory {

    def apply(data: ContractDescriptorData): SyncObjectContractFactory = new SyncObjectContractFactory(data)
}
