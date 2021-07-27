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

package fr.linkit.engine.connection.packet.persistence.v3.serialisation

object SerializerNodeFlags {

    val ByteFlag   : Byte = -128
    val ShortFlag  : Byte = -127
    val IntFlag    : Byte = -126
    val LongFlag   : Byte = -125
    val FloatFlag  : Byte = -124
    val DoubleFlag : Byte = -123
    val CharFlag   : Byte = -122
    val BooleanFlag: Byte = -121

    val StringFlag: Byte = -120
    val ArrayFlag : Byte = -119

    val HeadedValueFlag: Byte = -118
    val NullFlag       : Byte = -117

    val ObjectFlag: Byte = -116

}
