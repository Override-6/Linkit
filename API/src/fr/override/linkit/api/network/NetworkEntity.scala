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

package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.system.Version

import java.sql.Timestamp

trait NetworkEntity {

    val identifier: String

    val cache: SharedCacheHandler

    val connectionDate: Timestamp

    def addOnStateUpdate(action: ConnectionState => Unit): Unit

    def getConnectionState: ConnectionState

    def getProperty(name: String): Serializable

    def setProperty(name: String, value: Serializable): Unit

    def getRemoteConsole: RemoteConsole

    def getRemoteErrConsole: RemoteConsole

    def getApiVersion: Version

    def getRelayVersion: Version

    def listRemoteFragmentControllers: List[RemoteFragmentController]

    def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController]

}
