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

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.context.{Deconstructor, PersistenceContext}

object EmptyPersistenceContext extends PersistenceContext {

    private var network: Network = _

    override def findDeconstructor[T](clazz: Class[_]): Option[Deconstructor[T]] = None

    override def findConstructor[T](clazz: Class[_]): Option[java.lang.reflect.Constructor[T]] = None

    def initNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("Network already initialized !")
        this.network = network
    }

    override def getNetwork: Network = network
}
