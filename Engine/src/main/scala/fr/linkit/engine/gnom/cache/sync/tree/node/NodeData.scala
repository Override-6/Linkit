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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.DefaultConnectedObjectTree

class NodeData[+A <: AnyRef](val reference: ConnectedObjectReference, //The sync object reference
                             val presence: NetworkObjectPresence, //the sync object presence
                             val tree: DefaultConnectedObjectTree[_], //The object's tree
                             val currentIdentifier: String, //the current identifier
                             val ownerID: String, //the owner Identifier
                             val parent: Option[MutableNode[_]]) { // the parent of the node

    def this(other: NodeData[A]) = {
        this(other.reference, other.presence, other.tree, other.currentIdentifier, other.ownerID, other.parent)
    }

}