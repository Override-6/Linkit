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

package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.behavior.BHVProperties
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData

private[linkit] trait Contract {
    def registerProperties(properties: BHVProperties): Unit

    def apply(name: String, properties: BHVProperties): ContractDescriptorData

    def apply(name: String, propertiesName: String): ContractDescriptorData

    def apply(name: String): ContractDescriptorData
}

object Contract extends Contract {
    private final var impl: Contract = _

    private[linkit] def setImpl(impl: Contract): Unit = {
        if (this.impl != null) throw new IllegalArgumentException()
        this.impl = impl
    }

    override def registerProperties(properties: BHVProperties): Unit = impl.registerProperties(properties)

    override def apply(name: String, properties: BHVProperties): ContractDescriptorData = impl.apply(name, properties)

    override def apply(name: String, propertiesName: String): ContractDescriptorData = impl.apply(name, propertiesName)

    override def apply(name: String): ContractDescriptorData  = impl.apply(name)
}
