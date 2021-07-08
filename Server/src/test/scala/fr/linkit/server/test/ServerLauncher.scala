/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.server.test

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Scanner

object ServerLauncher {

    private val DefaultServerID = "TestServer1"
    val HomeProperty   : String = "LinkitHome"
    val DefaultHomePath: String = System.getenv("LOCALAPPDATA") + s"${File.separator}Linkit${File.separator}"

    def main(args: Array[String]): Unit = {
        AppLogger.info(s"Running server with arguments '${args.mkString(" ")}'")

        //val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")
        val resourcesFolder         = getOrElse(args, "--home-path", getDefaultLinkitHome)

        val config           = new ServerApplicationConfigBuilder {
            override val resourceFolder: String = resourcesFolder
            pluginFolder = None //userDefinedPluginFolder
            mainPoolThreadCount = 2
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = DefaultServerID
                    override val port      : Int    = 48484
                    nWorkerThreadFunction = c => c + 1//one thread per connections.

                    configName = "config1"
                }
            }
        }
        val serverAppContext = ServerApplication.launch(config, getClass)
        AppLogger.trace(s"Build complete: $serverAppContext")

        serverAppContext.runLaterControl {
            val pluginManager = serverAppContext.pluginManager
            pluginManager.loadAllClass(Array(
                classOf[ControllerExtension]: Class[_ <: Plugin],
                classOf[DebugExtension]: Class[_ <: Plugin],
            ))
        }.throwNextThrowable()
        AppLogger.info("Server Application launched.")

    }

    private def getDefaultLinkitHome: String = {
        val homePath = System.getenv(HomeProperty)
        if (homePath != null)
            return homePath

        val scanner = new Scanner(System.in)
        println(s"Environment variable '$HomeProperty' is not set !")
        println(s"Would you like to set your own path for the Linkit Framework home ?")
        println(s"Say 'y' to set your custom path, or something else to use default path '$DefaultHomePath'")
        print("> ")
        val response       = scanner.nextLine()
        val linkitHomePath = if (response.startsWith("y")) {
            println("Enter your custom path (the folder will be created if it does not exists)")
            print("> ")
            scanner.nextLine()
        } else DefaultHomePath
        setEnvHome(linkitHomePath)
        println(s"Linkit home path has been set to $linkitHomePath.")
        println("(If this java process is a child from another process, such as an IDE")
        println("You may restart the mother process in order to complete the environment variable update)")

        linkitHomePath
    }

    private def setEnvHome(linkitHomePath: String): Unit = {
        Files.createDirectories(Path.of(linkitHomePath)) //ensure that the folder exists.

        val osName     = System.getProperty("os.name").takeWhile(_ != ' ').trim.toLowerCase
        val setCommand = osName match {
            case "windows" | "ubuntu" => "setx"
            case "linux"              => "export"
        }

        val s = '\"'
        new ProcessBuilder("cmd", "/c", setCommand, HomeProperty, s"$s$linkitHomePath$s")
                .inheritIO()
                .start()
                .waitFor()
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
