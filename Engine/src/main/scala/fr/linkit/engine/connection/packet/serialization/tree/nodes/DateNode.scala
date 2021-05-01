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
import fr.linkit.engine.local.utils.NumberSerializer

import java.util.Date

object DateNode extends NodeFactory[Date] {

    override def canHandle(clazz: Class[_]): Boolean = classOf[Date].isAssignableFrom(clazz)

    override def canHandle(info: ByteSeq): Boolean = info.classExists(canHandle)

    override def newNode(finder: SerialContext, profile: ClassProfile[Date]): SerialNode[Date] = {
        new DateSerialNode(profile)
    }

    override def newNode(finder: SerialContext, bytes: ByteSeq): DeserialNode[Date] = {
        new DateDeserialNode(bytes, finder.getProfile[Date])
    }

    private class DateSerialNode(profile: ClassProfile[Date]) extends SerialNode[Date] {

        override def serialize(t: Date, putTypeHint: Boolean): Array[Byte] = {
            profile.applyAllSerialProcedures(t)
            val i = t.getTime
            //println(s"long = ${i}")
            //println(s"classType = ${t.getClass.getName}")
            NumberSerializer.serializeInt(t.getClass.getName.hashCode) ++ NumberSerializer.serializeLong(i)
        }
    }

    private class DateDeserialNode(bytes: ByteSeq, profile: ClassProfile[Date]) extends DeserialNode[Date] {

        override def deserialize(): Date = {
            val long = NumberSerializer.deserializeLong(bytes, 4)
            //println(s"long = ${long}")

            val clazz = bytes.getHeaderClass
            val date  = clazz.getDeclaredConstructor(classOf[Long])
                    .newInstance(NumberSerializer.deserializeLong(bytes, 4))
                    .asInstanceOf[Date]
            profile.applyAllSerialProcedures(date)
            date
        }
    }

}
