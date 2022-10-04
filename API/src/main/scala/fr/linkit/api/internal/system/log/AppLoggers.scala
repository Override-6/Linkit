/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.internal.system.log

import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.logging.log4j.core.config.Configurator

object AppLoggers {
    
    
    Configurator.initialize(null, "log4j2-development.xml")
    
    final val LogFileProperty = "logfilename"
    
    final val App         = logger("Application")
    final val GNOM        = logger("GNOM")
    final val ConnObj     = logger("GNOM.ConnObj")
    final val COInv       = logger("GNOM.CO.Inv")
    final val Mappings    = logger("Mappings")
    final val Persistence = logger("Persistence")
    final val Traffic     = logger("Traffic")
    final val Resource    = logger("Resource")
    final val Compilation = logger("Compilation")
    final val Worker      = logger("Worker")
    final val Connection  = logger("Connection")
    final val Watchdog  = logger("Watchdog")


    private var init = false
    
    private def logger(name: String): Logger = {
        if (!init && System.getProperty(LogFileProperty) == null) {
            println(s"Warn: No logfile will be generated because no '$LogFileProperty' system property was found.")
            val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
            val config  = context.getConfiguration
            config.getLoggers.values().forEach(_.removeAppender("LogFile"))
            init = true
        }
        LogManager.getLogger(name)
    }
}
