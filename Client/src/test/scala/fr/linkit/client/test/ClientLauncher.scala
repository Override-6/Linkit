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

package fr.linkit.client.test

import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.client.ClientApplication
import fr.linkit.client.local.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.client.local.config.schematic.ScalaClientAppSchematic
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugPlugin
import org.jetbrains.annotations.NotNull

import java.io.File
import java.net.InetSocketAddress
import java.util.Scanner
import scala.util.Try

object ClientLauncher {
    val Port           : Int               = 48484
    val ServerAddress  : InetSocketAddress = new InetSocketAddress("192.168.1.19", Port)
    val Localhost      : InetSocketAddress = new InetSocketAddress("localhost", Port)
    val HomeProperty   : String            = "LinkitHome"
    val DefaultHomePath: String            = System.getenv("LOCALAPPDATA") + s"${File.separator}Linkit${File.separator}"

    def main(args: Array[String]): Unit = {
        AppLogger.info(s"Running client with arguments '${args.mkString(" ")}'")
        val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")

        val scanner     = new Scanner(System.in)
        /*println("Say 'y' to connect to localhost")
        print("> ")
        val isLocalhost = scanner.nextLine().startsWith("y")*/
        val address     = Localhost

        println("Choose an identifier")
        print("> ")
        val identifier = scanner.nextLine()

        println(s"Choose how much client will connect to $address")
        print("Nothing = 1 > ")
        val numberEntry = scanner.nextLine()
        val raidCount   = if (numberEntry.isEmpty) 1 else Try(numberEntry.toInt).getOrElse(0)

        val resourcesFolder = getOrElse(args, "--home-path", getDefaultLinkitHome)

        launch(userDefinedPluginFolder, address, identifier, resourcesFolder, raidCount)
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
        new File(linkitHomePath).createNewFile() //ensure that the folder exists.

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
               identifier0: String,
               @NotNull resourcesFolder0: String,
               raidCount: Int): Unit = {

        if (resourcesFolder0 == null) {
            throw new NullPointerException("Resources folder is null !")
        }

        val config = new ClientApplicationConfigBuilder {
            override val resourcesFolder: String = resourcesFolder0
            nWorkerThreadFunction = _ + 1
            loadSchematic = new ScalaClientAppSchematic {
                for (i <- 1 to raidCount) {
                    clients += new ClientConnectionConfigBuilder {
                        pluginFolder = None // Some(mainPluginFolder)
                        override val identifier   : String            = identifier0 + i
                        override val remoteAddress: InetSocketAddress = address
                    }
                }
            }
        }
        val client = ClientApplication.launch(config, getClass)
        AppLogger.trace(s"Build completed: $client")
        client.runLaterControl {
            val pluginManager = client.pluginManager
            pluginManager.loadAllClass(Array(
                classOf[ControllerExtension]: Class[_ <: Plugin],
                classOf[DebugPlugin]: Class[_ <: Plugin],
            ))
        }.throwNextThrowable()
        AppLogger.info("Client Application launched.")
    }
}
