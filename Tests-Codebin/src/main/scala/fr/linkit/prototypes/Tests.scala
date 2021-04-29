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
import fr.linkit.core.connection.network.cache.puppet.generation.PuppetWrapperClassGenerator
import fr.linkit.core.connection.network.cache.puppet.{PuppetClassFields, Puppeteer}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.serialization.DefaultSerializer
import fr.linkit.core.local.mapping.{ClassMapEngine, ClassMappings}
import fr.linkit.core.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.core.local.system.fsa.nio.NIOFileSystemAdapter
import fr.linkit.core.local.utils.ScalaUtils

import java.sql.Timestamp
import java.time.Instant

object Tests {

    private val fsa = LocalFileSystemAdapters.Nio

    doMappings()

    //private val generatedPuppet = getTestPuppet

    private val coords     = DedicatedPacketCoordinates(12, "TestServer1", "s1")
    private val attributes = SimplePacketAttributes.from("25L" -> 25L)
    private val packet     = ObjectPacket(Timestamp.from(Instant.now()))

    def main(args: Array[String]): Unit = {

        ClassMappings.getClassOpt(classOf[Timestamp].getName.hashCode()).get

        val ref   = Array(coords, attributes, packet)
        val bytes = DefaultSerializer.serialize(ref, true)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (l: ${bytes.length})")
        val result = DefaultSerializer.deserializeAll(bytes)
        println(s"result = ${result.mkString("Array(", ", ", ")")}")
        println(s"result(1).getAttribute(25L) = ${result(1).asInstanceOf[SimplePacketAttributes].getAttribute("25L")}")
        println(s"result(2).toLocalDateTime = ${result(2).asInstanceOf[ObjectPacket].casted[Timestamp].toLocalDateTime}")
        println(s"result(2) = ${result(2).asInstanceOf[ObjectPacket].casted[Timestamp]}")
    }

    private def doMappings(): Unit = {
        ClassMapEngine.mapAllSourcesOfClasses(fsa, Seq(getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        ClassMapEngine.mapJDK(fsa)
    }

    private def getTestPuppet: NIOFileSystemAdapter = {
        val clazz = PuppetWrapperClassGenerator.getOrGenerate(classOf[NIOFileSystemAdapter])
        clazz.getConstructor(classOf[Puppeteer[_]], classOf[NIOFileSystemAdapter]).newInstance(new Puppeteer[NIOFileSystemAdapter](
            null,
            null,
            -4,
            "stp",
            PuppetClassFields.ofClass(classOf[NIOFileSystemAdapter]
            )), LocalFileSystemAdapters.Nio)
    }
}
