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

package fr.linkit.engine.connection.packet.persistence.serializor

object ConstantProtocol {
    val ContextRef: Byte = -2
    val PoolRef: Byte = -1

    val Null: Byte = 0
    val String: Byte = 1
    val Int: Byte = 2
    val Short: Byte = 3
    val Long: Byte = 4
    val Byte: Byte = 5
    val Double: Byte = 6
    val Float: Byte = 7
    val Boolean: Byte = 8
    val Char: Byte = 9
    val Object: Byte = 10

    val Array: Byte = 11


}
