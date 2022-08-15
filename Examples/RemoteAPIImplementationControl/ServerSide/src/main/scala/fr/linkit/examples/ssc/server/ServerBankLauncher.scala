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

import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.api.gnom.cache.sync.instantiation.New
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
        val network    = connection.network
        val global     = network.globalCaches
        val  contract   = Contract("BankControl", ObjectsProperty.defaults(network))
        val cache      = global.attachToCache(51, ConnectedObjectCache[UserAccountContainer](contract))
        cache.mirrorObject(0, New[UserAccountContainerImpl]())
    }

    private def launchApp(): ServerConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourcesFolder: String = System.getenv("LinkitHome")
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = ServerIdentifier
                    override val port      : Int    = ServerPort
                }
            }
        }
        ServerApplication.launch(config, classOf[UserAccountContainer])
                .findConnection(ServerIdentifier)
                .get
    }

}
