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

package fr.`override`.linkit.api.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager

import java.sql.Timestamp

trait NetworkEntity extends Updatable {

    val identifier: String

    val cache: SharedCacheManager

    val network: Network

    def connectionDate: Timestamp

    //def apiVersion: Version

    //def relayVersion: Version

    def getConnectionState: ConnectionState

    //def getRemoteConsole: RemoteConsole

    //def getRemoteErrConsole: RemoteConsole

    //def listRemoteFragmentControllers: List[RemoteFragmentController]

    //def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController]

    def toString: String

}
