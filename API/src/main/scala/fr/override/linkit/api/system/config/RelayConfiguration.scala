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

package fr.`override`.linkit.api.system.config

import fr.`override`.linkit.api.system.fsa.FileSystemAdapter
import fr.`override`.linkit.api.system.security.RelaySecurityManager

trait RelayConfiguration {

    val enableExtensionsFolderLoad: Boolean
    val enableTasks: Boolean
    val enableEventHandling: Boolean
    val enableRemoteConsoles: Boolean
    val checkReceivedPacketTargetID: Boolean

    val taskQueueSize: Int
    val maxPacketLength: Int //only concern custom bytes length
    val defaultContainerPacketCacheSize: Int //numbers of packet a PacketContainer can contain
    val maxPacketContainerCacheSize: Int //max registered PacketContainer in a TrafficHandler

    val securityManager: RelaySecurityManager
    val fsAdapter: FileSystemAdapter

    val extensionsFolder: String //can be relative or global
    val identifier: String

}
