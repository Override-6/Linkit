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

package fr.linkit.engine.connection.packet.persistence.serializor.read

import fr.linkit.engine.connection.packet.persistence.pool.PacketObjectPool

class DeserializerPacketObjectPool(sizes: Array[Int]) extends PacketObjectPool(sizes) {

    freeze() //Deserializer can't append objects, because sizes are already defined
    chunks.foreach(_.freeze())

    private val globalShifts = {
        val length = chunks.length
        val array  = new Array[Int](length)
        var i      = 1
        while (i < length) {
            val chunkSize = sizes(i)
            if (i == 1)
                array(i) = sizes(i - 1) + chunkSize
            else
                array(i) = array(i - 1) + chunkSize
            i += 1
        }
        array
    }

    def getAny(globalIdx: Int): Any = {
        val (tpe, shift) = getIdxTypeAndShift(globalIdx)
        chunks(tpe).get(globalIdx - shift)
    }

    private def getIdxTypeAndShift(globalIdx: Int): (Byte, Int) = {
        val shifts = globalShifts
        if (globalIdx < shifts(1)) {
            //globalIdx is a an index into the first chunk, so it does not needs to be shifted
            return (0, 0)
        }
        var i   = 1
        val len = shifts.length
        while (i < len - 1) {
            val nextShift = shifts(i + 1)
            if (globalIdx <= nextShift)
                return ((i.toByte + 1).toByte, shifts(i) + 1)
            i += 1
        }
        (i.toByte, shifts(i))
    }

}
