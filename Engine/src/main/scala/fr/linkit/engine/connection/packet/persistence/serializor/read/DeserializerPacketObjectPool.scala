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
import fr.linkit.engine.connection.packet.persistence.serializor.ConstantProtocol._

class DeserializerPacketObjectPool(sizes: Array[Int]) extends PacketObjectPool(sizes) {

    freeze() //Deserializer can't append objects, because sizes are already defined
    chunks.foreach(_.freeze())

    private val chunksPositions = {
        val length       = chunks.length
        val array        = new Array[Int](length)
        var currentIndex = 0
        var i            = 0
        while (i < length) {
            val chunkSize = chunks(i).size
            if (chunkSize == 0)
                array(i) = -1
            else {
                array(i) = currentIndex
                currentIndex += chunkSize
            }
            i += 1
        }
        array
    }

    def getType(globalPos: Int): Class[_] = {
        var pos = globalPos
        var tpeChunk = getChunkFromFlag[Class[_]](Class)
        val size = tpeChunk.size
        if (pos >= size) {
            tpeChunk = getChunkFromFlag[Class[_]](SyncClass)
            pos -= size
        }
        tpeChunk.get(pos)
    }

    @inline
    def getAny(globalPos: Int): Any = {
        if (globalPos == 0)
            return null
        val (tpe, shift) = getIdxTypeAndShift(globalPos - 1)
        chunks(tpe).get(globalPos - shift - 1)
    }

    private def getIdxTypeAndShift(globalIdx: Int): (Byte, Int) = {
        val positions = chunksPositions
        if (globalIdx < chunks(0).size) {
            //globalIdx is a an index into the first chunk, so it does not needs to be shifted
            return (0, 0)
        }
        var i   = 1
        val len = positions.length
        while (i < len - 1) {
            val nextPos        = positions(i + 1)
            val pos            = positions(i)
            val nextCompatible = nextPos == -1 && globalIdx - pos < sizes(i)
            if (nextCompatible || globalIdx < nextPos)
                return (i.toByte, pos)
            i += 1
        }
        (i.toByte, positions(i))
    }

}
