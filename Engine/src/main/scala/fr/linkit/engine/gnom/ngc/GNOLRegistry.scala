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

package fr.linkit.engine.gnom.ngc

import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.AsyncPacketChannel

import java.lang.ref.{Reference, WeakReference}
import scala.collection.mutable

class GNOLRegistry(network: Network) {
    private val gnol                        = network.gnol.asInstanceOf[GeneralNetworkObjectLinkerImpl]
    private val channel: AsyncPacketChannel = network.connection.traffic.getInjectable(11, AsyncPacketChannel, ChannelScopes.broadcast)
    private val references                  = mutable.HashMap.empty[Reference[_ <: DNO], NetworkObjectReference]

    private val signals = mutable.HashMap.empty[NetworkObjectReference, SignalHandler]

    listenSignals()

    private def listenSignals(): Unit = {
        channel.addOnPacketReceived(bundle => {
            val sender = network.findEngine(bundle.coords.senderID).get
            bundle.packet match {
                case AnyRefPacket(ref: NetworkObjectReference) => onSignal(sender, ref)
                case _                                         =>
                    throw UnexpectedPacketException(s"unknown packet signal ${bundle.packet}")
            }
        })
    }

    def onSignal(sender: Engine, reference: NetworkObjectReference): Unit = {
        val remove = signals.getOrElseUpdate(reference, new SignalHandler(reference)).signal(sender)
        if (remove) signals -= reference
    }

    def informCleaned(key: Reference[_ <: DNO]): Boolean = references.get(key) match {
        case Some(reference) =>
            channel.send(AnyRefPacket(reference))
            onSignal(network.currentEngine, reference)
            true
        case None            => false
    }

    def registerObject(dno: DNO): Unit = {
        references(new WeakReference[DNO](dno)) = dno.reference
    }


    class SignalHandler(nor: NetworkObjectReference) {
        private val deadSides = mutable.HashSet.empty[Engine]

        def signal(sender: Engine): Boolean = {
            deadSides += sender
            val presentNowere = network.listEngines.forall(deadSides.contains)
            if (presentNowere) {
                gnol.unregister(nor)
            }
            presentNowere
        }

    }

}

