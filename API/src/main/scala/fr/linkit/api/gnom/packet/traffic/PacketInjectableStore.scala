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

package fr.linkit.api.gnom.packet.traffic

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference

import scala.reflect.ClassTag

trait PacketInjectableStore extends TrafficObject[TrafficReference] {
    
    val defaultPersistenceConfig: PersistenceConfig
    
    override def reference: TrafficReference
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): InjectableTrafficNode[C] = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): InjectableTrafficNode[C] = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): InjectableTrafficNode[C]
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): InjectableTrafficNode[C] = {
        getInjectable[C](injectableID, config, factory, scopeFactory)
    }
    
    def findStore(id: Int): Option[PacketInjectableStore]
    
    def findInjectable[C <: PacketInjectable : ClassTag](id: Int): Option[C]
    
    def createStore(id: Int): PacketInjectableStore = createStore(id, defaultPersistenceConfig)
    
    def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore
    
}
