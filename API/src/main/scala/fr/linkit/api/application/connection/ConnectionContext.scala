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

package fr.linkit.api.application.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.gnom.referencing.StaticNetworkObject
import fr.linkit.api.internal.concurrency.Procrastinator

trait ConnectionContext extends StaticNetworkObject[NetworkConnectionReference.type] with Procrastinator {
    
    override val reference = NetworkConnectionReference
    
    val currentName: String
    
    def port: Int
    
    def traffic: PacketTraffic
    
    def getApp: ApplicationContext
    
    val translator: ObjectTranslator
    
    def network: Network
    
    def shutdown(): Unit
    
    def isAlive: Boolean
}
