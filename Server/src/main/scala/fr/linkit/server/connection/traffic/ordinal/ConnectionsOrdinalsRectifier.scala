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

package fr.linkit.server.connection.traffic.ordinal

import fr.linkit.api.gnom.packet.traffic.{InjectableTrafficNode, PacketTraffic}
import fr.linkit.engine.gnom.packet.traffic.unit.SequentialInjectionProcessorUnit

import java.util
import scala.collection.mutable

class ConnectionsOrdinalsRectifier(traffic: PacketTraffic) {
    
    private val shifts = mutable.HashMap.empty[Int, Counter]
    
    def getOrdinalShift(path: Array[Int]): Counter = shifts.getOrElseUpdate(util.Arrays.hashCode(path), {
        traffic.findNode(path) match {
            case Some(node: InjectableTrafficNode[_]) => node.unit() match {
                case unit: SequentialInjectionProcessorUnit =>
                    new Counter(unit.getCurrentOrdinal)
                case _                                      => new Counter()
            }
            case _                                    => new Counter()
        }
    })
    
}
