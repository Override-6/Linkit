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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.SharedCacheFactory
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData

import scala.reflect.ClassTag

trait ConnectedObjectCacheFactories {

    def apply[A <: AnyRef : ClassTag]: SharedCacheFactory[ConnectedObjectCache[A]]

    def apply[A <: AnyRef : ClassTag](contracts: ContractDescriptorData): SharedCacheFactory[ConnectedObjectCache[A]]
}
