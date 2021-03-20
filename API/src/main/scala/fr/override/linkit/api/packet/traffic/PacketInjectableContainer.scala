/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory

import scala.reflect.ClassTag

trait PacketInjectableContainer {

    /**
     * retrieves or create (and register) a [[PacketInjectable]] depending on the requested id and scope
     *
     * @param injectableID the injectable identifier
     * @param scopeFactory the scope factory that determines which relay can receive or send a packet to the injectable
     * @param factory      the factory of the injectable that will create the instance if needed.
     * @return an injectable matching the given identifier and scope
     * @see [[ChannelScope]]
     * @see [[PacketChannel]]
     * */
    def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C


}
