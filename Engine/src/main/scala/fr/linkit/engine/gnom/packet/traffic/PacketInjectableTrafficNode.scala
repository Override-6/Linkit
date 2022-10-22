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

import fr.linkit.api.gnom.packet.traffic.unit.InjectionProcessorUnit
import fr.linkit.api.gnom.packet.traffic.{InjectableTrafficNode, PacketInjectable, PacketTraffic}
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.engine.gnom.packet.traffic.unit.{PerformantInjectionProcessorUnit, SequentialInjectionProcessorUnit}
import fr.linkit.engine.internal.debug.cli.SectionedPrinter

import java.io.PrintStream

case class PacketInjectableTrafficNode[+C <: PacketInjectable](override val injectable       : C,
                                                               override val persistenceConfig: PersistenceConfig,
                                                               private val traffic           : PacketTraffic) extends InjectableTrafficNode[C] {

    private final val sipu = new SequentialInjectionProcessorUnit(injectable)
    private final val pipu = new PerformantInjectionProcessorUnit(injectable)
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

    override def chainTo(path: Array[Int]): this.type = {
        traffic.findNode(path).get match {
            case node: InjectableTrafficNode[_] => node.unit() match {
                case unit: SequentialInjectionProcessorUnit => sipu.chainWith(unit)
                case _                                      =>
            }
            case _                              =>
                throw new UnsupportedOperationException(s"Cannot chain to channel (${TrafficReference / path}). turns out that the reference is referring to a non Injectable Traffic Node")
        }
        this
    }

    override def unit(): InjectionProcessorUnit = if (preferPerformances0) pipu else sipu

    private[traffic] def dump(printer: SectionedPrinter)(section: printer.Section): Unit = {
        import printer._
        val unit = this.unit()
        section.append(injectable.trafficPath.mkString("/", "/", ""))
        section.append(": <unit: " + unit.shortname + s"> (of channel ${injectable.reference})\n")
        unit match {
            case sipu: SequentialInjectionProcessorUnit => sipu.appendDump(printer)(section, 6)
            case _                                      =>
        }
        section.flush()
    }
}
