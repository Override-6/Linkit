/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache

import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.ContentHandler
import fr.linkit.api.gnom.cache.{CacheContent, SharedCache}
import fr.linkit.engine.gnom.packet.AbstractAttributesPresence

abstract class AbstractSharedCache(channel: CachePacketChannel) extends AbstractAttributesPresence with SharedCache {

    private  val manager         = channel.manager
    override val family : String = manager.family
    override val cacheID: Int    = channel.cacheID

    override def update(): this.type = {
        if (manager == null)
            return this

        val content = channel.getCacheOfOwner
        val handler = channel.getHandler
        if (handler.isDefined) {
            handler.get match { //TODO ensure that the content cache is the expected type.
                case c: ContentHandler[CacheContent] => c.initializeContent(content)
                case _                               => //simply do nothing
            }
        }
        this
    }

}
