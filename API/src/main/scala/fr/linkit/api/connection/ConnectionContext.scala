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

package fr.linkit.api.connection

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.PacketTranslator
import fr.linkit.api.connection.packet.traffic.{PacketInjectableStore, PacketTraffic}
import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.concurrency.{ProcrastinatorControl, workerExecution}

trait ConnectionContext extends PacketInjectableStore with ProcrastinatorControl {

    val currentIdentifier: String

    def port: Int

    def traffic: PacketTraffic

    def getApp: ApplicationContext

    val translator: PacketTranslator

    def network: Network


    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean
}
