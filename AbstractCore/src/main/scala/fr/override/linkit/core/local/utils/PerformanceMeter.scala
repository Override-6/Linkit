/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.utils

class PerformanceMeter {

    private var reference = now

    def printPerf(): Unit = {
        val t = now
        println(s"time passed between now and last reference : ${t - reference}")
        reference = now
    }

    def printPerf(cause: String): Unit = {
        val t = now
        println(s"time passed between now and last reference : ${t - reference} ($cause)")
        reference = now
    }

    private def now: Long = System.currentTimeMillis()

}
