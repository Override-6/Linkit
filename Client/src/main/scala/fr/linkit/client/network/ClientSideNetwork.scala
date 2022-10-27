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
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.client.cache.ClientSharedCacheManager
import fr.linkit.client.connection.traffic.ClientPacketTraffic
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.engine.gnom.network.AbstractNetwork.GlobalCacheID
import fr.linkit.engine.gnom.network.{AbstractNetwork, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic

class ClientSideNetwork(traffic: AbstractPacketTraffic) extends AbstractNetwork(traffic) {


    override protected def createNewCache0(family: String, managerChannelPath: Array[Int]): SharedCacheManager = {
        val store = getStore(managerChannelPath)
        new ClientSharedCacheManager(family, this, objectManagementChannel, store)
    }

    override protected def retrieveDataTrunk(): NetworkDataTrunk = {
        val trunk = globalCaches.attachToCache(0, DefaultConnectedObjectCache[NetworkDataTrunk](this), CacheSearchMethod.GET_OR_CRASH)
                .findObject(0)
                .getOrElse {
                    throw new NoSuchElementException("network data trunk not found.")
                }
        trunk
    }
    override def serverName: String = traffic.serverName

}