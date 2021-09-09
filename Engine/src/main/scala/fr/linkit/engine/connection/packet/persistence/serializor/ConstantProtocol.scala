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
    final val Class: Byte = 0
    final val ContextRef: Byte = 1
    final val PoolRef: Byte = 2

    final val String: Byte = 3
    final val Int: Byte = 4
    final val Short: Byte = 5
    final val Long: Byte = 6
    final val Byte: Byte = 7
    final val Double: Byte = 8
    final val Float: Byte = 9
    final val Boolean: Byte = 10
    final val Char: Byte = 11

    final val Array: Byte = 12
    final val Object: Byte = 13

    final val Null: Byte = -128
}
