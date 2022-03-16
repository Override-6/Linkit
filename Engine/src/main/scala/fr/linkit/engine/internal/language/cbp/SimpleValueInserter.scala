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

package fr.linkit.engine.internal.language.cbp

import fr.linkit.api.internal.language.cbp.ValueInserter
import fr.linkit.engine.internal.utils.ArrayByteInserter
import org.jetbrains.annotations.NotNull

class SimpleValueInserter(val posInScopeBp: Int, @NotNull val source: String) extends ArrayByteInserter(source.getBytes) with ValueInserter {

    override def recenterPosFromScopeBp(pos: Int): Int = {
        val recenter = pos - posInScopeBp
        if (recenter > origin.length || recenter < 0)
            -1
        else recenter
    }

    override def insertValue(value: String, valueName: String, pos: Int): Unit = {
        if (isIgnoredPosition(pos))
            return
        val valueClauseLength = valueName.length + 2
        val shift             = getShiftAt(pos)
        buff.remove(pos + shift, valueClauseLength)
        buff.insertAll(pos + shift, value.getBytes())
        val posShift = value.length - valueClauseLength
        archiveShift(pos, posShift)
    }

    override def insertOther(inserter: ValueInserter, pos: Int): Unit = {
        val shift = getShiftAt(pos)
        val block = inserter.getStringResult.getBytes
        buff.insertAll(pos + shift, block)
        archiveShift(pos, block.length)
    }

    override def insertBlock(block: String, pos: Int): Unit = {
        val shift = getShiftAt(pos)
        buff.insertAll(pos + shift, block.getBytes())
        archiveShift(pos, block.length)
    }

    override def getStringResult: String = new String(buff.toArray)
}
