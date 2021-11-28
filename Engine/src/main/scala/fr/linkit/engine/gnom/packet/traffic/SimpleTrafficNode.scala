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

import fr.linkit.api.gnom.packet.traffic.{TrafficNode, TrafficObject}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference

case class SimpleTrafficNode[C <: TrafficObject[TrafficReference]](override val injectable: C,
                                                                   override val persistenceConfig: PersistenceConfig) extends TrafficNode[C] {

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
}
