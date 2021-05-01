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

import fr.linkit.engine.connection.packet.serialization.tree.SerialContext.ClassProfile
import fr.linkit.engine.connection.packet.serialization.tree._

object NullNode extends NodeFactory[Null] {

    val NullFlag: Array[Byte] = Array(-74)

    override def canHandle(clazz: Class[_]): Boolean = clazz == null

    override def canHandle(bytes: ByteSeq): Boolean = bytes.sameFlag(NullFlag(0))

    override def newNode(finder: SerialContext, profile: ClassProfile[Null]): SerialNode[Null] = {
        new NullSerial()
    }

    override def newNode(finder: SerialContext, bytes: ByteSeq): DeserialNode[Null] = {
        new NullDeserial()
    }

    class NullSerial() extends SerialNode[Null] {

        override def serialize(t: Null, putTypeHint: Boolean): Array[Byte] = NullFlag
    }

    class NullDeserial() extends DeserialNode[Null] {

        override def deserialize(): Null = null
    }
}
