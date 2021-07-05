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

package fr.linkit.api.local.generation.cbp

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
