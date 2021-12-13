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

package fr.linkit.examples.ssc.server

import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.examples.ssc.api.UserAccountContainer
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}
import fr.linkit.server.connection.ServerConnection

import scala.io.StdIn

object ServerBankLauncher {

    private final val ServerPort       = 48481
    private final val ServerIdentifier = "BankServer"

    def main(args: Array[String]): Unit = {
        val container = createAccounts()
        while (true) {
            val input = StdIn.readLine()
            if (input == "stop")
                return
        }
    }

    private def createAccounts(): UserAccountContainer = {
        val connection = launchApp()
        val global     = connection.network.globalCache
        val cache      = global.attachToCache(51, DefaultSynchronizedObjectCache[UserAccountContainer]())
        cache.syncObject(0, Constructor[UserAccountContainerImpl]())
    }

    private def launchApp(): ServerConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourceFolder: String = "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\Linkit\\Home"
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = ServerIdentifier
                    override val port      : Int    = ServerPort
                }
            }
        }
        ServerApplication.launch(config, getClass)
                .findConnection(ServerIdentifier)
                .get
    }

}
