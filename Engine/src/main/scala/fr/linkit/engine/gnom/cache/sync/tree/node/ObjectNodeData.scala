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
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference, SynchronizedObjectTree}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import org.jetbrains.annotations.Nullable

class ObjectNodeData[A <: AnyRef](val puppeteer: Puppeteer[A], //Remote invocations
                                  val chip: Chip[A], //Reflective invocations
                                  val contract: SynchronizedStructureContract[A],
                                  val synchronizedObject: A with SynchronizedObject[A], //the sync object
                                  @Nullable val origin: AnyRef) //The synchronized object's origin (the same object before it was converted to its synchronized version, if any).
                                 (reference: SyncObjectReference, //The sync object reference
                                  presence: NetworkObjectPresence, //the sync object presence
                                  currentIdentifier: String,
                                  tree: SynchronizedObjectTree[_], //The node's tree
                                  parent: Option[MutableSyncNode[_]]) //identifier of the current engine.
    extends NodeData[A](reference, presence, tree, currentIdentifier, puppeteer.ownerID, parent) {
}