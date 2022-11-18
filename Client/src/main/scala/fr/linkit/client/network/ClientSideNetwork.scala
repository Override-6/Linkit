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

package fr.linkit.client.network

import fr.linkit.api.gnom.cache.{CacheSearchMethod, SharedCacheManager}
import fr.linkit.api.gnom.network.tag.{NameTag, NetworkFriendlyEngineTag, Server, UniqueTag}
import fr.linkit.client.cache.ClientSharedCacheManager
import fr.linkit.client.connection.ClientConnection
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}

class ClientSideNetwork(connection: ClientConnection) extends AbstractNetwork(connection) {


    val serverNameTag = connection.boundNT

    override def retrieveNT(uniqueTag: UniqueTag with NetworkFriendlyEngineTag): NameTag = uniqueTag match {
        //add Server case (super.retrieveNT would also support the Server case but this override supports Server case during trunk initialization)
        case Server => serverNameTag
        case _      => super.retrieveNT(uniqueTag)
    }

    override protected def createNewCache0(family: String, managerChannelPath: Array[Int]): SharedCacheManager = {
        val store = getStore(managerChannelPath)
        new ClientSharedCacheManager(family, this, traffic.getObjectManagementChannel, store)
    }

    override protected def retrieveDataTrunk(): NetworkDataTrunk = {
        val trunk = globalCaches.attachToCache(0, DefaultConnectedObjectCache[NetworkDataTrunk](this), CacheSearchMethod.GET_OR_CRASH)
                .findObject(0)
                .getOrElse {
                    throw new NoSuchElementException("network data trunk not found.")
                }
        trunk
    }


}