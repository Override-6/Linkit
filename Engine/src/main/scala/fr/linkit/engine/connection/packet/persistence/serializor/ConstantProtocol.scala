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
    val ContextRef: Byte = -115
    val PoolRef: Byte = -114

    val Null: Byte = -128
    val String: Byte = -127
    val Int: Byte = -126
    val Short: Byte = -125
    val Long: Byte = -124
    val Byte: Byte = -123
    val Double: Byte = -122
    val Float: Byte = -121
    val Boolean: Byte = -120
    val Char: Byte = -119

    val Object: Byte = -118
    val Array: Byte = -117
}
