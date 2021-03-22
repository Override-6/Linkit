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

package fr.`override`.linkit.core.local.system

import fr.`override`.linkit.api.local.ApplicationContext
import org.apache.log4j.{Logger, Priority}


object ContextLogger {

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

    def log(priority: Priority, msg: AnyRef): Unit = logger.log(priority, msg)
    def log(priority: Priority, msg: AnyRef, throwable: Throwable): Unit = logger.trace(priority, msg, throwable)

}
