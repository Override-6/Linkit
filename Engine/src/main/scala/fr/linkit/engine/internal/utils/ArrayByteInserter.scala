/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.utils

import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArrayByteInserter(@NotNull val origin: Array[Byte]) {

    protected val buff            : ListBuffer[Byte]       = ListBuffer.from(origin)
    protected val insertionHistory         = mutable.HashMap.empty[Int, Int] //(Index, Shift)
    protected val ignoredBPZones  : ListBuffer[(Int, Int)] = ListBuffer.empty[(Int, Int)]

    def isIgnoredPosition(pos: Int): Boolean = {
        ignoredBPZones.exists(zone => pos >= zone._1 && pos <= zone._2)
    }

    def insertBytes(pos: Int, bytes: Array[Byte]): Unit = {
        val shift = getShiftAt(pos)
        buff.insertAll(pos + shift, bytes)
        archiveShift(pos, bytes.length)
    }

    def deleteBlock(pos: Int, len: Int): Unit = {
        val shift = getShiftAt(pos)
        buff.remove(pos + shift, len)
        archiveShift(pos + len, -len)
        ignoredBPZones += ((pos, pos + len))
    }

    def getShiftAt(pos: Int): Int = {
        insertionHistory.takeWhile(_._1 < pos).values.sum
    }

    protected def archiveShift(pos: Int, shift: Int): Unit = {
        def insert(shift: Int): Unit = {
            insertionHistory(pos) = shift
        }
        //insert(shift)
        insertionHistory.get(pos).fold(insert(shift)) { (posShift) =>
            insert(shift + posShift)
        }
    }

    def getResult: Array[Byte] = buff.toArray


}