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

package fr.`override`.linkit.api.connection

import fr.`override`.linkit.api.connection.network.Network
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.connection.packet.traffic.{PacketInjectableContainer, PacketTraffic}
import fr.`override`.linkit.api.local.concurrency.{IllegalThreadException, Procrastinator, workerExecution}
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier

trait ConnectionContext extends PacketInjectableContainer with Procrastinator {
    val configuration: ConnectionConfiguration

    val supportIdentifier: String

    def traffic: PacketTraffic

    def translator: PacketTranslator

    def network: Network

    def eventNotifier: EventNotifier

    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean
}
