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

package fr.linkit.server

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

object ServerLauncher {

    def main(args: Array[String]): Unit = {
        AppLogger.info(s"Running server with arguments '${args.mkString(" ")}'")
        val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")

        val config           = new ServerApplicationConfigBuilder {
            pluginsFolder = None //userDefinedPluginFolder
            mainPoolThreadCount = 1
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = "TestServer1"
                    override val port      : Int    = 48484
                    nWorkerThreadFunction = _ + 1 //Two threads per connections.

                    configName = "config1"
                }
            }
        }
        val serverAppContext = ServerApplication.launch(config, getClass)
        AppLogger.trace(s"Build complete: $serverAppContext")
        val pluginManager = serverAppContext.pluginManager
        pluginManager.loadAllClass(Array(
            classOf[ControllerExtension]: Class[_ <: Plugin],
            classOf[DebugExtension]: Class[_ <: Plugin],
        ))
    }

    //noinspection SameParameterValue
    private def getOrElse(args: Array[String], key: String, defaultValue: String): String = {
        val index = args.indexOf(key)
        if (index < 0 || index + 1 > args.length - 1) {
            defaultValue
        } else {
            args(index + 1)
        }
    }

}
