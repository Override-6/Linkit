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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.env.ChippedObjectCompanion
import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.ChippedObjectAdapter.addChippedObject
import fr.linkit.engine.internal.util.Identity

import scala.collection.mutable
import scala.util.Try

final class ChippedObjectAdapter[A <: AnyRef](override val connected: A) extends ChippedObject[A] {

    private var choreographer: InvocationChoreographer  = _
    private var reference0   : ConnectedObjectReference = _
    private var presence0    : NetworkObjectPresence     = _
    private var node         : ChippedObjectCompanion[A] = _

    override val isMirrored: Boolean = true //pure chipped objects are always mirrored.

    override def getClassDef: SyncClassDef = SyncClassDef(connected.getClass)

    override def reference: ConnectedObjectReference = reference0

    override def isInitialized: Boolean = node != null

    /**
     * this object's node.
     * */
    override def getCompanion: ChippedObjectCompanion[A] = node

    /**
     * @return the invocation choreographer of this object
     * @see InvocationChoreographer
     */
    override def getChoreographer: InvocationChoreographer = choreographer

    /**
     * @return true because chipped objects that are not SynchronizedObjects can only be present
     *         on their owning engine. If this object is sent to another engine, the resulting object will become a
     *         mirroring object. The mirroring object's sync implementation is specified by the contract descriptor data.
     */
    override def isOrigin: Boolean = true

    override def presence: NetworkObjectPresence = presence0

    def initialize(node: ChippedObjectCompanion[A]): Unit = {
        if (node.contract.mirroringInfo.isEmpty)
            throw new IllegalConnectedObjectException("Pure chipped object's contract must define mirroring information.")
        this.choreographer = node.choreographer
        this.reference0 = node.reference
        this.presence0 = node.presence
        this.node = node
        addChippedObject(this)
    }

}

object ChippedObjectAdapter {
    private val map = mutable.HashMap.empty[Identity[Any], ChippedObject[_]]

    private def addChippedObject(chippedObject: ChippedObject[_]): Unit = {
        map.put(Identity(chippedObject.connected), chippedObject)
    }

    def findAdapter(obj: Any): Option[ChippedObject[_]] = {
        if (obj == null) return None
        Try(map.get(Identity(obj))).toOption.flatten
    }
}
