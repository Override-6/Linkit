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

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference

import scala.reflect.ClassTag

trait PacketInjectableStore extends TrafficObject[TrafficReference] {
    
    val defaultPersistenceConfig: PersistenceConfig
    
    override def reference: TrafficReference
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): TrafficNode[C] = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): TrafficNode[C] = {
        getInjectable[C](injectableID, defaultPersistenceConfig, factory, scopeFactory)
    }
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): TrafficNode[C]
    
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, scopeFactory: ScopeFactory[_ <: ChannelScope])(implicit factory: PacketInjectableFactory[C]): TrafficNode[C] = {
        getInjectable[C](injectableID, config, factory, scopeFactory)
    }
    
    def findStore(id: Int): Option[PacketInjectableStore]
    
    def findInjectable[C <: PacketInjectable : ClassTag](id: Int): Option[C]
    
    def createStore(id: Int): PacketInjectableStore = createStore(id, defaultPersistenceConfig)
    
    def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore
    
}
