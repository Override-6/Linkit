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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficNetworkPresenceReference
import fr.linkit.api.gnom.reference.NetworkObjectReference

import scala.reflect.ClassTag

trait PacketInjectableStore extends TrafficPresence[TrafficNetworkPresenceReference] {

    override lazy val reference: TrafficNetworkPresenceReference = new TrafficNetworkPresenceReference(trafficPath)

    val defaultPersistenceConfig: PersistenceConfig

    /**
     * retrieves or create (and register) a [[PacketInjectable]] depending on the requested id and scope
     *
     * @param injectableID the injectable identifier
     * @param scopeFactory the scope factory that determines which engine can receive or send a packet to the injectable
     * @param factory      the factory of the injectable that will create the instance if needed.
     * @return an injectable matching the given identifier and scope
     * @see [[ChannelScope]]
     * @see [[PacketChannel]]
     * */
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }

    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): C = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }

    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C

    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): C = {
        getInjectable[C](injectableID, config, factory, scopeFactory)
    }

    def findStore(id: Int): Option[PacketInjectableStore]

    def createStore(id: Int): PacketInjectableStore = createStore(id, defaultPersistenceConfig)

    def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore

}
