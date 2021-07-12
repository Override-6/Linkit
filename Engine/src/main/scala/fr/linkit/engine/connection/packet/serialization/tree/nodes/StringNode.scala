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

package fr.linkit.engine.connection.packet.serialization.tree.nodes

import fr.linkit.api.connection.packet.serialization.tree._

object StringNode extends NodeFactory[String] {

    private val StringFlag: Array[Byte] = Array(-101)

    override def canHandle(clazz: Class[_]): Boolean = clazz == classOf[String]

    override def canHandle(bytes: ByteSeq): Boolean = bytes.sameFlagAt(0, StringFlag(0))

    override def newNode(finder: NodeFinder, profile: ClassProfile[String]): SerialNode[String] = {
        new StringSerialNode(profile)
    }

    override def newNode(finder: NodeFinder, bytes: ByteSeq): DeserialNode[String] = {
        new StringDeserialNode(finder.getProfile[String], bytes)
    }

    class StringSerialNode(profile: ClassProfile[String]) extends SerialNode[String] {

        override def serialize(t: String, putTypeHint: Boolean): Array[Byte] = {
            profile.applyAllSerialProcedures(t)
            StringFlag ++ t.getBytes()
        }
    }

    class StringDeserialNode(profile: ClassProfile[String], bytes: Array[Byte]) extends DeserialNode[String] {

        override def deserialize(): String = {
            val result = new String(bytes.drop(1))
            profile.applyAllDeserialProcedures(result)
            result
        }
    }

}
