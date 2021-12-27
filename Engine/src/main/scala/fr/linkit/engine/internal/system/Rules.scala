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

package fr.linkit.engine.internal.system

import java.util.regex.Pattern

object Rules {

    final val MaxConnectionIDLength: Int  = 16
    final val ConnectionRefused    : Byte = 1
    final val ConnectionAccepted   : Byte = 2

    final val WPArgsLength   : Byte        = 1
    final val WPArgsSeparator: Array[Byte] = ";".getBytes()

    final val IdentifierPattern: Pattern = Pattern.compile("[^;]{0,1024}$")

}
