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

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.packet.traffic.{TrafficNode, PacketInjectable}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig

trait InternalPacketInjectableStore {

    def getPersistenceConfig(path: Array[Int], pos: Int = 0): PersistenceConfig

    def findNode(path: Array[Int], pos: Int = 0): Option[TrafficNode[PacketInjectable]]

}
