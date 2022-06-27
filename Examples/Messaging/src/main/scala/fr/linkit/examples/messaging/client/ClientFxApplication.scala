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

package fr.linkit.examples.messaging.client

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.gnom.network.Network
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache
import fr.linkit.examples.messaging.Channel

import java.net.InetSocketAddress

class ClientFxApplication extends Application {
    
    def main(args: Array[String]): Unit = {
        val pseudonym = args.head
        
        val network : Network                       = connectToNetwork(pseudonym)
        val channels: ConnectedObjectCache[Channel] = network.globalCaches
                .attachToCache("channels".##, DefaultConnectedObjectCache[Channel])
        
        val mainChannel: Channel = channels.findObject("main".##).get
        val window = new ChannelWindow(mainChannel, pseudonym)
        startFxApp(window)
    }
    
    def startFxApp(window: ChannelWindow): Unit = ()
    
    def connectToNetwork(pseudonym: String): Network = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier    = pseudonym
                    override val remoteAddress = new InetSocketAddress("localhost", 48482)
                }
            }
        }
        ClientApplication.launch(config, getClass)
                .findConnection(pseudonym)
                .get.network
    }
    
}
