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

import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier

trait ContractDescriptorGroup[A <: AnyRef] {
    
    val clazz      : Class[A]
    val modifier   : Option[ValueModifier[A]]
    val descriptors: Array[StructureContractDescriptor[A]]
}

object ObjectContractDescriptorGroup extends ContractDescriptorGroup[Object] {
    
    override val clazz      : Class[Object]                              = classOf[Object]
    override val modifier   : Option[ValueModifier[Object]]              = None
    override val descriptors: Array[StructureContractDescriptor[Object]] = Array(new EmptyStructureContractDescriptor(clazz))
}
