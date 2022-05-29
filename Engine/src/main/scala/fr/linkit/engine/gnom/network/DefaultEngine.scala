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

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.network._
import fr.linkit.api.internal.system.Versions
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.internal.mapping.RemoteClassMappings
import fr.linkit.engine.internal.system.StaticVersions

import java.sql.Timestamp

class DefaultEngine(override val identifier: String,
                    override val cache: SharedCacheManager,
                    network0: AbstractNetwork) extends Engine {
    
    override val reference: EngineReference = new EngineReference(identifier)
    override val network  : Network         = network0
    
    //val reference: EngineReference = new EngineReference(identifier)
    override val versions: Versions = StaticVersions.currentVersions
    
    private var mappings              : Option[RemoteClassMappings] = None
    private var isMappingsInitializing: Boolean                     = false
    
    def isCurrentEngine = identifier == network.connection.currentIdentifier
    
    def classMappings: Option[RemoteClassMappings] = {
        if (mappings.isEmpty && !isMappingsInitializing) network0.mappingsCache match {
            case None        =>
            case Some(cache) =>
                isMappingsInitializing = true
                mappings = {
                    if (isCurrentEngine) {
                        AppLoggers.Persistence.info(s"Class mappings of this engine has been sent over the network.")
                        Some(cache.syncObject(identifier.hashCode, Constructor[RemoteClassMappings](identifier)))
                    } else {
                        val r = cache.findObject(identifier.hashCode)
                        if (r.isDefined)
                            AppLoggers.Persistence.info(s"Established connection with class mappings of '$identifier'.")
                        r
                    }
                }
                isMappingsInitializing = false
        }
        mappings
    }
    
    override def toString: String = if (isCurrentEngine) s"CurrentEngine($identifier)" else s"DistantEngine($identifier)"
    
    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())
    
    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED
    
    def updateState(state: ExternalConnectionState): Unit = connectionState = state
    
    override def getConnectionState: ExternalConnectionState = connectionState
    
}
