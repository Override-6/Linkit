import fr.linkit.api.local.plugin.Plugin
import fr.linkit.api.local.system.AppLogger
import fr.linkit.plugin.controller.ControllerExtension
import fr.linkit.plugin.debug.DebugExtension
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.function.{Executable, ThrowingSupplier}
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.io.File
import java.util.Scanner

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

import java.util.concurrent.locks.LockSupport

@TestInstance(Lifecycle.PER_CLASS)
class ServerLauncher {

    private val DefaultServerID = "TestServer1"
    val HomeProperty   : String = "LinkitHome"
    val DefaultHomePath: String = System.getenv("LOCALAPPDATA") + s"${File.separator}Linkit${File.separator}"

    private var application: ServerApplication = _

    @Test
    def launch(): Unit = {
        //AppLogger.info(s"Running server with arguments '${args.mkString(" ")}'")

        //val userDefinedPluginFolder = getOrElse(args, "--plugin-path", "/Plugins")
        val resourcesFolder = getLinkitHomePath

        val config = new ServerApplicationConfigBuilder {
            override val resourceFolder: String = resourcesFolder
            pluginsFolder = None //userDefinedPluginFolder
            mainPoolThreadCount = 2
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = DefaultServerID
                    override val port      : Int    = 48484
                    nWorkerThreadFunction = _ + 1 //Two threads per connections.

                    configName = "config1"
                }
            }
        }
        application = Assertions.assertDoesNotThrow {
            ServerApplication.launch(config, getClass)
        }
        AppLogger.info(s"Launch complete: $application")

        Assertions.assertAll("App launch conclusion",
            Assertions.assertNotNull(application),
        )


    }

    @Test
    def plugins(): Unit = {

        if (application == null)
            fail()
        application.runLaterControl {
            val pluginManager = application.pluginManager
            pluginManager.loadAllClass(Array(
                classOf[ControllerExtension]: Class[_ <: Plugin],
                classOf[DebugExtension]: Class[_ <: Plugin],
            ))
        }.join()

        AppLogger.info("Server Application ready to use.")
    }

    private def getLinkitHomePath: String = {
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

    private implicit def toThrowingSupplier[T](action: => T): ThrowingSupplier[T] = () => action

    private implicit def toExecutable[T](action: => T): Executable = () => action
}
