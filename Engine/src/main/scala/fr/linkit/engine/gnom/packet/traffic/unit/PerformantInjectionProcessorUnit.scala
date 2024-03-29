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

package fr.linkit.engine.gnom.packet.traffic.unit

import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic.PacketInjectable
import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.persistence.{PacketDownload, PacketUpload}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.SimplePacketBundle

//performance is simplicity
class PerformantInjectionProcessorUnit(injectable: PacketInjectable) extends InjectionProcessorUnit {


    /**
     * a short name for this type of unit
     * */
    override val shortname: String = "PIPU"

    override def post(result: PacketDownload): Unit = {
        AppLoggers.GNOM.trace(s"PIPU: adding packet injection for channel '${injectable.reference}'.")
        
        result.makeDeserialization()
        val bundle = SimplePacketBundle(result)
        injectable.inject(bundle)
    }
}
