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

import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.reference.{NetworkObjectLinker, NetworkObjectReference}

case class WriterInfo(traffic: PacketTraffic,
                      persistenceConfig: PersistenceConfig,
                      path: Array[Int],
                      private val gnol0: (() => NetworkObjectLinker[NetworkObjectReference])) {
    lazy val gnol: NetworkObjectLinker[NetworkObjectReference] = gnol0.apply()
}
