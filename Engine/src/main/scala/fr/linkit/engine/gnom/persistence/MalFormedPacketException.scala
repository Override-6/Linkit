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

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.packet.PacketException

import java.nio.ByteBuffer
import scala.collection.mutable

case class MalFormedPacketException(bytes: Array[Byte], msg: String) extends PacketException(msg) {

    def this(buffer: ByteBuffer, msg: String) {
        this(if (buffer == null) null else buffer.array().slice(buffer.position(), buffer.limit()), msg)
    }

    def this(msg: String) {
        this(null: Array[Byte], msg)
    }

    override protected def appendMessage(sb: mutable.StringBuilder): Unit = {
        super.appendMessage(sb)
        if (bytes != null)
            sb.append(s"For sequence: ${new String(bytes)}")
    }

}
