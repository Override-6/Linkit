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

package fr.linkit.core.local.system

import fr.linkit.api.local.ApplicationContext
import org.apache.log4j.{Level, Logger}
import org.jetbrains.annotations.Nullable

object AppLogger {

    var NetworkPreviewLength: Int = 1000

    val logger: Logger = Logger.getLogger(classOf[ApplicationContext])

    def trace(msg: AnyRef): Unit = logger.trace(msg)

    def trace(msg: AnyRef, throwable: Throwable): Unit = logger.trace(msg, throwable)

    def info(msg: AnyRef): Unit = logger.info(msg)

    def info(msg: AnyRef, throwable: Throwable): Unit = logger.info(msg, throwable)

    def warn(msg: AnyRef): Unit = logger.warn(msg)

    def warn(msg: AnyRef, throwable: Throwable): Unit = logger.warn(msg, throwable)

    def error(msg: AnyRef): Unit = logger.error(msg)

    def error(msg: AnyRef, throwable: Throwable): Unit = logger.error(msg, throwable)

    def fatal(msg: AnyRef): Unit = logger.fatal(msg)

    def fatal(msg: AnyRef, throwable: Throwable): Unit = logger.fatal(msg, throwable)

    def debug(msg: AnyRef): Unit = logger.debug(msg)

    def debug(msg: AnyRef, throwable: Throwable): Unit = logger.debug(msg, throwable)

    def logUpload(target: String, bytes: Array[Byte]): Unit = {
        if (logger.isDebugEnabled) {
            val preview = new String(bytes.take(NetworkPreviewLength)).replace('\n', ' ').replace('\r', ' ')
            debug(s"${Console.MAGENTA}Written : ↑ $target ↑ $preview (l: ${bytes.length})")
        }
    }

    def logDownload(@Nullable target: String, bytes: Array[Byte]): Unit = {
        if (logger.isDebugEnabled) {
            val preview     = new String(bytes.take(NetworkPreviewLength)).replace('\n', ' ').replace('\r', ' ')
            val finalTarget = if (target == null) "" else target
            debug(s"${Console.CYAN}Received: ↓ $finalTarget ↓ $preview (l: ${bytes.length})")
        }
    }

    def log(level: Level, msg: AnyRef): Unit = logger.log(level, msg)

    def log(level: Level, msg: AnyRef, throwable: Throwable): Unit = logger.log(level, msg, throwable)

}
