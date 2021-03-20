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

package fr.`override`.linkit.api.system

import org.apache.log4j.{Level, Logger, Priority}

class RelayLogger(name: String) extends Logger(name) {

    override def forcedLog(fqcn: String, level: Priority, message: Any, t: Throwable): Unit = {
        val prefix: String = "" //TODO getPrefix(level)
        super.forcedLog(fqcn, level, prefix + message, t)
    }

}

object RelayLogger {

    import Level._

    private val DefaultPrefix = ""

    private val Prefixes: Map[Priority, String] = Map(
        DEBUG -> "(DEBUG)",
        TRACE -> Console.BLUE,
        WARN -> Console.YELLOW,
        ERROR -> Console.RED,
        FATAL -> Console.RED_B
    )

    def getPrefix(level: Priority): String = {
        Prefixes.getOrElse(level, DefaultPrefix)
    }

}

