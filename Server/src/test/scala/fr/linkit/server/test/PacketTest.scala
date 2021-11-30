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

package fr.linkit.server.test

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.DedicatedPacketCoordinates
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.reference.linker.GeneralNetworkObjectLinker
import fr.linkit.api.gnom.reference.traffic.ObjectManagementChannel
import fr.linkit.engine.gnom.cache.sync.generation.rectifier.SyncClassRectifier.typeStringClass
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl
import fr.linkit.engine.gnom.packet.SimplePacketAttributes
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.gnom.packet.traffic.channel.DefaultObjectManagementChannel
import fr.linkit.engine.gnom.packet.traffic.{ChannelScopes, DynamicSocket, SocketPacketWriter, WriterInfo}
import fr.linkit.engine.gnom.persistence.context.{ImmutablePersistenceContext, PersistenceConfigBuilder}
import fr.linkit.engine.gnom.persistence.{DefaultObjectTranslator, PacketSerializationChoreographer, SimpleTransferInfo}
import fr.linkit.engine.gnom.reference.linker.WeakContextObjectLinker
import fr.linkit.engine.internal.manipulation.invokation.{ConstructorInvoker, ObjectInvocator}
import fr.linkit.engine.internal.utils.{ClassMap, ScalaUtils}
import fr.linkit.server.connection.ServerConnection
import fr.linkit.server.connection.packet.ServerPacketTraffic
import fr.linkit.server.test.PacketTest.serialAndDeserial
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}
import org.mockito.Mockito
import java.util

import fr.linkit.engine.internal.manipulation.creation.ObjectCreator

@TestInstance(Lifecycle.PER_CLASS)
class PacketTest {
    PacketTest // Load statics

    @Test
    def other(): Unit = {
        val obj = ObjectCreator.allocate(classOf[JavaObject])
        val constructor = classOf[JavaObject].getConstructors.head
        val invoker = new ConstructorInvoker(constructor)
        var args: Array[Any] = Array[Any](1, 2, 3, 4, 5, 6, true, 0.5, 0.2)
        invoker.invoke(obj, args)
        println("done")
        println(s"obj = ${obj}")

        args = Array[Any](Long.MaxValue, Double.MinValue, Char.MaxValue, Double.MinValue, Double.MaxValue, 60, 3, 0.50, 0.200)
        invoker.invoke(obj, args)
        println("done")
        println(s"obj = ${obj}")

    }

    @Test
    def serialAndDeserialList(): Unit = {
        val tested = new util.HashSet[(String, Int)]() {
            Seq("Strings" -> 1, "To" -> 0, "Consider" -> -1, "Cool" -> -2).foreach(add)
        }
        serialAndDeserial(tested)
    }


}

object PacketTest {

    import Mockito._

    private val app                              = ServerLauncher.launch()
    private val gnol: GeneralNetworkObjectLinker = mock(classOf[GeneralNetworkObjectLinkerImpl])
    private val connection                       = mock(classOf[ServerConnection])
    private val network                          = mock(classOf[Network])
    when(connection.getApp)
            .thenReturn(app)
    when(connection.network)
            .thenReturn(network)
    when(network.gnol)
            .thenReturn(gnol)
    private val traffic: PacketTraffic           = new ServerPacketTraffic(connection, None)
    private val translator                       = new DefaultObjectTranslator(app)
    private val coords                           = DedicatedPacketCoordinates(Array(7, 8, 9), "you", "me")
    private val attributes                       = SimplePacketAttributes.empty
    private val omc    : ObjectManagementChannel = {
        val writer = new SocketPacketWriter(mock(classOf[DynamicSocket]), new PacketSerializationChoreographer(translator), WriterInfo(traffic, null, Array.empty, () => network))
        new DefaultObjectManagementChannel(null, ChannelScopes.include().apply(writer))
    }
    private val config                           = {
        val script  = getClass.getResource("/default_scripts/persistence_minimal.sc")
        val context = ImmutablePersistenceContext(new ClassMap(), new ClassMap())
        val linker  = new WeakContextObjectLinker(null, omc)
        PersistenceConfigBuilder.fromScript(script, traffic)
                .build(context, linker, omc)
    }

    def serialAndDeserial(obj: AnyRef): Unit = {
        println(s"Serializing and deserializing object $obj")
        val serialResult = translator.translate(SimpleTransferInfo(coords, attributes, ObjectPacket(obj), config, gnol))
        val buff         = serialResult.buff
        println("Packet bytes: " + ScalaUtils.toPresentableString(buff) + s" (size: ${buff.limit()} bytes)")
        buff.position(4)
        val deserialResult = translator.translate(traffic, buff)
        val result         = deserialResult.packet.asInstanceOf[ObjectPacket].value
        println(s"Deserialization result: $result")
    }

}
