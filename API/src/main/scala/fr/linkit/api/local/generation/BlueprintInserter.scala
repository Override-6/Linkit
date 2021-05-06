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

package fr.linkit.api.local.generation

import scala.collection.mutable

class BlueprintInserter(builder: StringBuilder) {

    private val insertionHistory = mutable.ListBuffer.empty[(Int, Int)] //(Index, Shift)

    def insertValue(value: String, valueName: String, pos: Int): Unit = {
        val valueClauseLength = valueName.length + 2
        val shift             = getShift(pos)
        builder.replace(pos + shift, pos + valueClauseLength + shift, value)
        archiveShift(pos, value.length - valueClauseLength)
    }

    def insertBlock(block: String, pos: Int): Unit = {
        val shift = getShift(pos)
        builder.insert(pos + shift, block)
        archiveShift(pos, block.length)
    }

    def deleteBlock(pos: Int, len: Int): Unit = {
        val shift = getShift(pos)
        builder.delete(pos + shift, pos + len + shift)
        archiveShift(pos, len - pos)
    }

    def getResult: String = builder.toString()

    def getShift(pos: Int): Int = {
        insertionHistory.takeWhile(_._1 < pos).map(_._2).sum
    }

    private def archiveShift(pos: Int, shift: Int): Unit = {
        insertionHistory.insert(insertionHistory.lastIndexWhere(_._1 <= pos) + 1, (pos, shift))
    }

}
