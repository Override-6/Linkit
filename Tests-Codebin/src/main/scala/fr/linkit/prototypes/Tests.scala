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
import fr.linkit.engine.connection.cache.repo.generation.PuppetWrapperClassGenerator
import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.serialization.DefaultSerializer
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacket
import fr.linkit.engine.local.mapping.{ClassMapEngine, ClassMappings}
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.engine.local.system.fsa.nio.{NIOFileAdapter, NIOFileSystemAdapter}
import fr.linkit.engine.local.utils.ScalaUtils

import java.nio.file.Path
import java.sql.Timestamp

object Tests {

    private val fsa = LocalFileSystemAdapters.Nio

    doMappings()

    //private val generatedPuppet = getTestPuppet

    private val coords     = DedicatedPacketCoordinates(12, "TestServer1", "s1")
    private val attributes = SimplePacketAttributes.apply("cache" -> 27, "id" -> -192009448, "family" -> "s1")
    private val packet     = RequestPacket(9, Array(ObjectPacket(Array(Path.of("C:\\Users\\maxim\\Desktop\\fruits")))))

    def main(args: Array[String]): Unit = {

        ClassMappings.findClass(classOf[Timestamp].getName.hashCode()).get

        val serializer = new DefaultSerializer
        val ref   = Array(coords, attributes, packet)
        val bytes = serializer.serialize(ref, true)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (l: ${bytes.length})")
        val result = serializer.deserializeAll(bytes)
        println(s"result = ${result.mkString("Array(", ", ", ")")}")
    }

    private def doMappings(): Unit = {
        ClassMapEngine.mapAllSourcesOfClasses(fsa, Seq(getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        ClassMapEngine.mapJDK(fsa)
    }

    private def getTestPuppet: Unit = {
        /*val clazz = PuppetWrapperClassGenerator.getOrGenerate(classOf[NIOFileSystemAdapter])
        clazz.getConstructor(classOf[Puppeteer[_]], classOf[NIOFileSystemAdapter]).newInstance(new Puppeteer[NIOFileSystemAdapter](
            null,
            null,
            -4,
            "stp",
            PuppetClassDesc.ofClass(classOf[NIOFileSystemAdapter]
            )), LocalFileSystemAdapters.Nio)*/
    }
}
