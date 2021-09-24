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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.network.{Network, NetworkInitialisable}
import fr.linkit.api.connection.packet.persistence.context.{Deconstructor, PersistenceContext}
import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.api.local.ApplicationContext
import fr.linkit.engine.local.utils.ClassMap

import java.lang.reflect.Constructor

class ImmutablePersistenceContext private(override val traffic: PacketTraffic,
                                          constructors: ClassMap[Constructor[_]],
                                          deconstructor: ClassMap[Deconstructor[_]]) extends PersistenceContext with NetworkInitialisable {


    override def findConstructor[T](clazz: Class[_]): Option[java.lang.reflect.Constructor[T]] = {
        constructors.get(clazz)
                .asInstanceOf[Option[java.lang.reflect.Constructor[T]]]
    }

    override def findDeconstructor[T](clazz: Class[_]): Option[Deconstructor[T]] = {
        deconstructor.get(clazz)
                .asInstanceOf[Option[Deconstructor[T]]]
    }

    private var network: Network = _

    override def initNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("Network already initialized !")
        this.network = network
    }

    override def getNetwork: Network = network
}

object ImmutablePersistenceContext {

    def apply(traffic: PacketTraffic, constructors: ClassMap[Constructor[_]], deconstructor: ClassMap[Deconstructor[_]]): ImmutablePersistenceContext = {
        new ImmutablePersistenceContext(traffic, new ClassMap(constructors), new ClassMap(deconstructor))
    }
}
