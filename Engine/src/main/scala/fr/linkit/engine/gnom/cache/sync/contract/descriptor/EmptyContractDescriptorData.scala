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

package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorGroup, OverallStructureContractDescriptor}

object EmptyContractDescriptorData extends ContractDescriptorDataImpl(Array(ObjectContractDescriptorGroup), "<empty>")

private object ObjectContractDescriptorGroup extends ContractDescriptorGroup[Object] {
    
    override val clazz       = classOf[Object]
    override val modifier    = None
    override val descriptors = Array(new OverallStructureContractDescriptor[Object] {
        override val autochip    = false
        override val targetClass = classOf[Object]
        override val methods     = Array()
        override val fields      = Array()
    })
}
