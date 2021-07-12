/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.serialization.procedure

import fr.linkit.api.connection.cache.CacheSearchBehavior
import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.serialization.tree.procedure.Procedure
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter

object PuppetWrapperProcedure extends Procedure[PuppetWrapper[Serializable]] {

    private var failCount = 0

    override def beforeSerial(t: PuppetWrapper[Serializable], network: Network): Unit = {
        //NO-OP
    }

    override def afterDeserial(wrapper: PuppetWrapper[Serializable], network: Network): Unit = {
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val family        = puppeteerDesc.cacheFamily
        val cacheID       = puppeteerDesc.cacheID
        network.getCacheManager(family).fold {
            AppLogger.warn(s"${wrapper.getClass.getName}: Received packet containing puppet that belongs to cache family '$family' which is not opened on this machine.")
            AppLogger.warn(s"Therefore, this object will be lost and will not be synchronised as it was probably expected.")
            if (failCount % 10 == 0) {
                AppLogger.warn(s"Tip: You can open the cache manager '$family' then open a ${classOf[DefaultEngineObjectCenter[_]].getSimpleName} with cache identifier '$cacheID")
                AppLogger.warn(s"     In order to retrieve this object.")
            }
            failCount += 1
        } { cache =>
            val repo = cache.getCacheAsync(cacheID, DefaultEngineObjectCenter[Serializable](), CacheSearchBehavior.GET_OR_CRASH)
            repo.initPuppetWrapper(wrapper)
        }
    }
}
