/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.system

import java.util.regex.Pattern

object Rules {

    val MaxConnectionIDLength: Int  = 16
    val ConnectionRefused    : Byte = 1
    val ConnectionAccepted   : Byte = 2

    val WPArgsLength   : Byte        = 3
    val WPArgsSeparator: Array[Byte] = ";".getBytes()

    val IdentifierPattern: Pattern = Pattern.compile("[^;]{0,1024}$")

}
