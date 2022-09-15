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

package fr.linkit.test

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.internal.system.security.ApplicationSecurityManager
import fr.linkit.client.config.ClientConnectionConfigBuilder
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.StringPacket
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.gnom.packet.traffic.channel.SyncPacketChannel
import fr.linkit.mock.application.LinkitApplicationMock
import fr.linkit.server.config.ServerConnectionConfigBuilder
import org.junit.jupiter.api.Test

import java.net.InetSocketAddress
import scala.util.chaining.scalaUtilChainingOps

class TestEngine {
    
    @Test
    def launchMockingApp(): Unit = {
        val application = LinkitApplicationMock.launch(new ApplicationConfiguration {
            override val resourceFolder : String                     = getWorkingDir
            override val securityManager: ApplicationSecurityManager = ApplicationSecurityManager.None
        })
        val server      = application.openServerConnection(new ServerConnectionConfigBuilder {
            override val identifier: String = "Server"
            override val port      : Int    = 49490
        })
        
        val client = application.openClientConnection(new ClientConnectionConfigBuilder {
            override val identifier   : String            = "Client"
            override val remoteAddress: InetSocketAddress = new InetSocketAddress("localhost", 49490)
        })
    
        val serverChannel = server.traffic.getInjectable(188, SyncPacketChannel, ChannelScopes.include("Client"))
    
        val clientChannel = client.traffic.getInjectable(188, SyncPacketChannel, ChannelScopes.include("Server"))
        
        serverChannel.send(StringPacket("Hello !"))
        clientChannel.nextPacket[StringPacket].pipe(println)
    }
    
    private def getWorkingDir: String = {
        val envValue = System.getenv("LinkitHome")
        if (envValue == null)
            throw new NoSuchElementException("LinkitHome property not set. Please set a path in LinkitHome property to start persistence tests")
        envValue
    }
    
}
