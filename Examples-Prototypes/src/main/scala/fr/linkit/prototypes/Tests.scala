/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.prototypes

import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.{ArrayRefPacket, ObjectPacket}
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.serialization.v2.tree.ClassTree
import fr.linkit.core.connection.packet.traffic.channel.request.ResponsePacket
import fr.linkit.core.local.system.fsa.JDKFileSystemAdapters

import java.sql.Timestamp
import java.time.Instant

object Tests {

    private val coords     = DedicatedPacketCoordinates(12, "s1", "TestServer1")
    private val packet     = WrappedPacket("Hey", WrappedPacket("World", ObjectPacket(Array(("How", 15), ("Are", 50), ("freaking", 48), "you?"))))
    private val attributes = SimplePacketAttributes.empty

    private val fsa = JDKFileSystemAdapters.Nio

    def main(args: Array[String]): Unit = {
        val tree = new ClassTree
        val node = tree.getNodeForRef(packet)
        val bytes = node.serialize(packet, true)
        println(s"new String(bytes) = ${new String(bytes)} (${bytes.length})")
    }

}
