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

import fr.linkit.api.connection.network.cache.CacheOpenBehavior
import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.api.local.ApplicationContext
import fr.linkit.core.connection.network.cache.puppet.generation.PuppetClassGenerator
import fr.linkit.core.connection.network.cache.puppet.{PuppetClassFields, Puppeteer}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.RefPacket.ArrayRefPacket
import fr.linkit.core.connection.packet.serialization.DefaultSerializer
import fr.linkit.core.connection.packet.traffic.channel.request.ResponsePacket
import fr.linkit.core.local.mapping.ClassMapEngine
import fr.linkit.core.local.system.fsa.LocalFileSystemAdapters
import fr.linkit.core.local.system.fsa.nio.NIOFileSystemAdapter
import fr.linkit.core.local.utils.ScalaUtils

object Tests {

    private val fsa = LocalFileSystemAdapters.Nio

    doMappings()

    private val generatedPuppet = getTestPuppet

    private val coords     = DedicatedPacketCoordinates(12, "TestServer1", "s1")
    private val packet     = ResponsePacket(7, Array(ArrayRefPacket(Array((-192009448, LocalFileSystemAdapters.Nio), (-192009448, generatedPuppet)))))
    private val attributes = SimplePacketAttributes.empty


    def main(args: Array[String]): Unit = {
        val ref = Array(coords, attributes, packet)
        val bytes  = DefaultSerializer.serialize(ref, true)
        println(s"bytes = ${ScalaUtils.toPresentableString(bytes)} (l: ${bytes.length})")
        val result = DefaultSerializer.deserializeAll(bytes)
        println(s"result = ${result.mkString("Array(", ", ", ")")}")
    }

    private def doMappings(): Unit = {
        ClassMapEngine.mapAllSourcesOfClasses(fsa, Seq(getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext]))
        ClassMapEngine.mapJDK(fsa)
    }


    private def getTestPuppet: NIOFileSystemAdapter = {
        val clazz = PuppetClassGenerator.getOrGenerate(classOf[NIOFileSystemAdapter])
        clazz.getConstructor(classOf[Puppeteer[_]], classOf[NIOFileSystemAdapter]).newInstance(new Puppeteer[NIOFileSystemAdapter](
            null,
            null,
            -4,
            "stp",
            PuppetClassFields.ofClass(classOf[NIOFileSystemAdapter]
            )), LocalFileSystemAdapters.Nio)
    }
}
