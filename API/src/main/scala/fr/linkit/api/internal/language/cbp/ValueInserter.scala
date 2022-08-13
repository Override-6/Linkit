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

package fr.linkit.api.internal.language.cbp

trait ValueInserter {

    def recenterPosFromScopeBp(pos: Int): Int

    def isIgnoredPosition(pos: Int): Boolean

    def insertValue(value: String, valueName: String, pos: Int): Unit

    def insertOther(inserter: ValueInserter, pos: Int): Unit

    def insertBlock(block: String, pos: Int): Unit

    def deleteBlock(pos: Int, len: Int): Unit

    def getStringResult: String

    def getShiftAt(pos: Int): Int

}
