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

package fr.linkit.api.connection.packet.serialization.tree

trait ByteSeq {

    val array: Array[Byte]

    def findClassOfSeq[T]: Option[Class[_]]

    def getClassOfSeq[T]: Class[T]

    def isClassDefined: Boolean

    def apply(i: Int): Byte

    def classExists(f: Class[_] => Boolean): Boolean

    def sameFlagAt(pos: Int, flag: Byte): Boolean

}

object ByteSeq {
    implicit def extractBytes(seq: ByteSeq): Array[Byte] = seq.array
}
