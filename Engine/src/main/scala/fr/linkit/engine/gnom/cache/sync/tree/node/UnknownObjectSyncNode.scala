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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.SynchronizedStructureContract
import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference, SynchronizedObjectTree}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence

class UnknownObjectSyncNode(override val objectPresence: NetworkObjectPresence,
                            override val reference: SyncObjectReference,
                            override val tree: SynchronizedObjectTree[_],
                            override val ownerID: String,
                            override val parent: SyncNode[_]) extends SyncNode[AnyRef] {

    override val contract          : SynchronizedStructureContract[AnyRef]  = null
    override val puppeteer         : Puppeteer[AnyRef]                      = null
    override val chip              : Chip[AnyRef]                           = null

    override val id                : Int                                    = reference.nodePath.last
    override val synchronizedObject: AnyRef with SynchronizedObject[AnyRef] = null
}
