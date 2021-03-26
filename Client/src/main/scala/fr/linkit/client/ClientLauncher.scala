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

package fr.linkit.client

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.core.local.system.ContextLogger
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension

import java.net.InetSocketAddress
import java.util.Scanner

object ClientLauncher {

    val PORT = 48484
    val SERVER_ADDRESS = new InetSocketAddress("192.168.1.19", PORT)
    val LOCALHOST = new InetSocketAddress("localhost", PORT)

    def main(args: Array[String]): Unit = {
        val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")


        print("say 'y' to connect to localhost : ")
        val scanner = new Scanner(System.in)
        val isLocalhost = scanner.nextLine().startsWith("y")
        val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS

        print("choose an identifier : ")
        val identifier = scanner.nextLine()

        launch(userDefinedPluginFolder, address, identifier)
    }

    private def getOrElse(args: Array[String], key: String, defaultValue: String): String = {
        val index = args.indexOf(key)
        if (index < 0 || index + 1 > args.length - 1) {
            defaultValue
        } else {
            args(index + 1)
        }

    }

    def launch(mainPluginFolder: String,
               address: InetSocketAddress,
               identifier0: String): Unit = {

        val config = new ClientApplicationConfigBuilder {
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    pluginFolder = None // Some(mainPluginFolder)
                    override val identifier: String = identifier0
                    override val remoteAddress: InetSocketAddress = address
                }
            }
        }
        val client = ClientApplication.launch(config)
        ContextLogger.debug(s"Build completed: $client")
        val pluginManager = client.pluginManager
        pluginManager.loadAllClass(Array(
            classOf[ControllerExtension]: Class[_ <: Plugin],
            classOf[DebugExtension]: Class[_ <: Plugin],
        ))
    }
}
