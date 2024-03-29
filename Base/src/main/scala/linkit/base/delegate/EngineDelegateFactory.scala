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

package linkit.base.delegate

import fr.linkit.api.application.resource.ResourceInternFactories
import fr.linkit.api.gnom.cache.sync.ConnectedObjectCacheFactories
import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.delegate.DelegateFactoryDelegate
import linkit.base.resource.EngineResourceInternFactories
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.internal.concurrency.VirtualProcrastinator
import fr.linkit.engine.internal.language.bhv.ContractProvider

class EngineDelegateFactory extends DelegateFactoryDelegate {
    override val defaultProcrastinatorSupplier: Procrastinator.Supplier = VirtualProcrastinator

    override def defaultCOCFactories: ConnectedObjectCacheFactories = DefaultConnectedObjectCache

    override def contracts: Contract.Provider = ContractProvider

    override def resourceFactories: ResourceInternFactories = EngineResourceInternFactories
}
