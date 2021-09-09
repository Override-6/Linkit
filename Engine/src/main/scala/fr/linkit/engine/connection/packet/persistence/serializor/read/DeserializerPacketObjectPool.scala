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

    private val globalShifts = {
        val length = chunks.length
        val array  = new Array[Int](length)
        var i      = 0
        while (i < length) {
            val chunkSize = sizes(i)
            if (i == 0)
                array(i) = chunkSize
            else
                array(i) = array(i - 1) + chunkSize
            i += 1
        }
        array
    }

    def getObject(globalIdx: Int): Any = {
        val tpe = getIdxType(globalIdx)
        chunks(tpe).get(globalIdx - tpe)
    }

    private def getIdxType(globalIdx: Int): Byte = {
        val shifts  = globalShifts
        var i: Byte = 0
        val len     = shifts.length
        while (i < len) {
            if (shifts(i) >= globalIdx && (i == len || shifts(i + 1) >= globalIdx))
                return i
            i += 1
        }
        i
    }

}
