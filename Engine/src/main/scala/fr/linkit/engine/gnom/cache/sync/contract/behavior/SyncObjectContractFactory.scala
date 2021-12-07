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

import fr.linkit.api.gnom.cache.sync.contract.SynchronizedStructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.{AgreementContext, SynchronizedObjectContractFactory}
import fr.linkit.engine.gnom.cache.sync.contract.builder.{ContractDescriptorData, ObjectBehaviorDescriptor}
import fr.linkit.engine.internal.utils.ClassMap

class SyncObjectContractFactory(descriptions: ContractDescriptorData) extends SynchronizedObjectContractFactory {


    override def getObjectContract[A <: AnyRef](clazz: Class[_], context: AgreementContext): SynchronizedStructureContract[A] = {
        descriptions.getNode(clazz).getContract(clazz, context: AgreementContext).asInstanceOf[SynchronizedStructureContract[A]]
    }


}
