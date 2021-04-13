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
import fr.linkit.api.local.ApplicationContext
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.fundamental.WrappedPacket
import fr.linkit.core.connection.packet.serialization.LocalCachedObjectSerializer
import fr.linkit.core.connection.packet.serialization.v2.tree.ClassTree
import fr.linkit.core.local.mapping.ClassMapEngine
import fr.linkit.core.local.system.fsa.JDKFileSystemAdapters
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString

object Tests {

    private val coords     = DedicatedPacketCoordinates(12, "s1", "TestServer1")
    private val packet     = WrappedPacket("Hey", WrappedPacket("World", ObjectPacket(Array(("How", 15), ("Are", 50), ("freaking", 48), "you?"))))
    private val attributes = SimplePacketAttributes.empty

    private val fsa = JDKFileSystemAdapters.Nio

    def main(args: Array[String]): Unit = {
        doMappings()

        {
            val tree  = new ClassTree
            val node  = tree.getSerialNodeForRef(packet)
            val bytes = node.serialize(packet, true)
            println(s"New : String(bytes) = ${toPresentableString(bytes)} (l: ${bytes.length})")
            val result = tree.getDeserialNodeFor(bytes).deserialize()
            println(s"result = ${result}")
        }
        {
            val bytes = LocalCachedObjectSerializer.serialize(packet, true)
            println(s"Old : String(bytes) = ${toPresentableString(bytes)} (l: ${bytes.length})")
        }

        println(s"ORIGIN IS : $packet")
    }

    private def doMappings(): Unit = {
        ClassMapEngine.mapAllSourcesOfClasses(fsa, getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext])
        ClassMapEngine.mapJDK(fsa)
    }

}
