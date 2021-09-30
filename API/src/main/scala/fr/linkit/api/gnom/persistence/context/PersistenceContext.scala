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

package fr.linkit.api.gnom.persistence.context

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketTraffic

import java.lang.reflect.Constructor

trait PersistenceContext {

    val traffic: PacketTraffic

    def getNetwork: Network

    def findConstructor[T](clazz: Class[_]): Option[Constructor[T]]

    def findDeconstructor[T](clazz: Class[_]): Option[Deconstructor[T]]

}
