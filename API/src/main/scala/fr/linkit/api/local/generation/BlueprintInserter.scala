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

import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BlueprintInserter(val posInScopeBp: Int, @NotNull val blueprint: String) {

    val builder: StringBuilder = new StringBuilder(blueprint)
    private val insertionHistory = mutable.ListBuffer.empty[(Int, Int)] //(Index, Shift)
    private val ignoredBPZones   = ListBuffer.empty[(Int, Int)]

    def recenterPosFromScopeBp(pos: Int): Int = {
        val recenter = pos - posInScopeBp
        if (recenter > blueprint.length || recenter < 0)
            -1
        else recenter
    }

    def isIgnoredPosition(pos: Int): Boolean = {
        ignoredBPZones.exists(zone => pos >= zone._1 && pos <= zone._2)
    }

    def insertValue(value: String, valueName: String, pos: Int): Unit = {
        if (isIgnoredPosition(pos))
            return
        val valueClauseLength = valueName.length + 2
        val shift             = getShift(pos)
        builder.replace(pos + shift, pos + shift + valueClauseLength, value)
        archiveShift(pos, value.length - valueClauseLength)
    }

    def insertOther(inserter: BlueprintInserter, pos: Int): Unit = {
        val shift = getShift(pos)
        val block = inserter.getResult
        builder.insert(pos + shift, block)
        archiveShift(pos, block.length)
    } //#######################################################||||||||||||||||||||||||||
    //#############!!!!!####################||||||||||||||||||||||||||

    def insertBlock(block: String, pos: Int): Unit = {
        val shift = getShift(pos)
        builder.insert(pos + shift, block)
        archiveShift(pos, block.length)
    }

    def deleteBlock(pos: Int, len: Int): Unit = {
        val shift = getShift(pos)
        builder.delete(pos + shift, pos + len + shift)
        archiveShift(pos + len, -len)
        ignoredBPZones += ((pos, pos + len))
    }

    def getResult: String = builder.toString()

    def getShift(pos: Int): Int = {
        insertionHistory.takeWhile(_._1 < pos).map(_._2).sum
    }

    private def archiveShift(pos: Int, shift: Int): Unit = {
        def insert(shift: Int): Unit = {
            insertionHistory.insert(insertionHistory.lastIndexWhere(_._1 <= pos) + 1, (pos, shift))
        }
        //insert(shift)
        insertionHistory.find(_._1 == pos).fold(insert(shift)) { (pair) =>
            insert(shift + pair._2)
        }
    }

}
