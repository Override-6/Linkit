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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet.traffic.injection.InjectionProcessorUnit
import fr.linkit.api.gnom.packet.traffic.{PacketTraffic, TrafficNode, TrafficObject}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.engine.gnom.packet.traffic.injection.{PerformantInjectionProcessorUnit, SequentialInjectionProcessorUnit}

case class SimpleTrafficNode[C <: TrafficObject[TrafficReference]](override val injectable: C,
                                                                   override val persistenceConfig: PersistenceConfig,
                                                                   private val traffic: PacketTraffic) extends TrafficNode[C] {

    private final val sipu = new SequentialInjectionProcessorUnit()
    private final val pipu = new PerformantInjectionProcessorUnit()
    chainIPU(ObjectManagementChannelReference) //Chain with the OMC when sequential is used

    private var preferPerformances0 = false

    override def setPerformantInjection(): this.type = {
        preferPerformances0 = true
        this
    }

    override def setSequentialInjection(): this.type = {
        preferPerformances0 = false
        this
    }

    override def preferPerformances(): Boolean = preferPerformances0

    override def chainIPU(path: Array[Int]): this.type = {
        traffic.findNode(path).get.ipu() match {
            case unit: SequentialInjectionProcessorUnit => sipu.chainWith(unit)
            case _                                      => //FIXME Should always be able to chain with a SIPU. Here is a fast fix because i have to go xoxo
        }
        this
    }

    override def ipu(): InjectionProcessorUnit = if (preferPerformances0) pipu else sipu
}
