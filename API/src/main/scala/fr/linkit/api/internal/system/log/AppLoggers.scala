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

package fr.linkit.api.internal.system.log

import org.apache.logging.log4j.{LogManager, Logger}

object AppLoggers {
    
    final val Root        = LogManager.getRootLogger
    final val App         = logger("Application")
    final val GNOM        = logger("GNOM")
    final val SyncObj     = logger("GNOM.SyncObj")
    final val Mappings    = logger("Mappings")
    final val Persistence = logger("Persistence")
    final val Traffic     = logger("Traffic")
    final val Resource    = logger("Resource")
    final val Compilation = logger("Compilation")
    final val Worker      = logger("Worker")
    final val Connection  = logger("Connection")
    
    //Debug logger used to print stuff that are not pertinent for the user and that must be removed once the bug is fixed
    final val Debug = logger("Debug")
    
    private def logger(name: String): Logger = LogManager.getLogger(name)
}
