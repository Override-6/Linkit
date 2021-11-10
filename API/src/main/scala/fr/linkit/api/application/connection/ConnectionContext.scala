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

package fr.linkit.api.application.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.reference.{NetworkObject, StaticNetworkObject}
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.system.event.EventNotifier

trait ConnectionContext extends StaticNetworkObject[NetworkConnectionReference] with ProcrastinatorControl {

    val currentIdentifier: String

    override val reference: NetworkConnectionReference = NetworkConnectionReference

    def port: Int

    def traffic: PacketTraffic

    def getApp: ApplicationContext

    val translator: ObjectTranslator

    def network: Network

    def eventNotifier: EventNotifier

    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean
}
