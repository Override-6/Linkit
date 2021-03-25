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

package fr.`override`.linkit.server

import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.core.connection.packet.serialization.CompactedPacketTranslator
import fr.`override`.linkit.core.local.system.ContextLogger
import fr.`override`.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.`override`.linkit.server.config.{AmbiguityStrategy, ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}


object ServerLauncher {
    def main(args: Array[String]): Unit = {
        ContextLogger.info(s"Running server with arguments ${args.mkString("'", ", ", "'")}")
        val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")

        val config = new ServerApplicationConfigBuilder {
            pluginsFolder = userDefinedPluginFolder
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = "TestServer1"
                    override val port: Int = 48484

                    configName = "config1"
                }
            }
        }
        val serverApplicationContext = ServerApplication.launch(config)
        ContextLogger.trace(s"Build complete: $serverApplicationContext")
    }

    private def getOrElse(args: Array[String], key: String, defaultValue: String): String = {
        val index = args.indexOf(key)
        if (index < 0 || index + 1 > args.length - 1) {
            defaultValue
        } else {
            args(index + 1)
        }
    }

}
