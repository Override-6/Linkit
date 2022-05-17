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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.tree.ConnectedObjectNode
import fr.linkit.api.gnom.reference.DynamicNetworkObject

trait ConnectedObject[A <: AnyRef] extends DynamicNetworkObject[ConnectedObjectReference] {

    def connected: A

    /**
     * The unique reference of the object.
     * */
    def reference: ConnectedObjectReference

    /**
     * this object's node.
     * */
    def getNode: ConnectedObjectNode[A]

    /**
     * @return true if this object is the original among other copies of the same object on other clients.<br>
     *         An original object can <b>ONLY</b> be present on the current engine.
     */
    def isOrigin: Boolean

    /**
     * Note: a connected object is always initialized if it was retrieved normally.
     *
     * @return true if the object is initialized.
     */
    def isInitialized: Boolean

    def getClassDef: SyncClassDef

}