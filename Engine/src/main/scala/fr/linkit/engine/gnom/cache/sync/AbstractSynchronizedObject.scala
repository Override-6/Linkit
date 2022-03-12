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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.{InvocationChoreographer, MirroringObjectInvocationException}
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{ObjectSyncNode, SyncObjectReference}
import fr.linkit.api.gnom.cache.sync.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.node.ObjectSyncNodeImpl

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    protected final          var location         : SyncObjectReference     = _
    @transient private final var puppeteer        : Puppeteer[A]            = _
    @transient private final var contract         : StructureContract[A]    = _
    @transient private final var choreographer    : InvocationChoreographer = _
    @transient private final var presenceOnNetwork: NetworkObjectPresence   = _
    @transient private final var node             : ObjectSyncNode[A]       = _

    //cached for handleCall
    @transient private final var currentIdentifier: String  = _
    @transient private final var ownerID          : String  = _
    @transient private final var isNotMirroring   : Boolean = _

    def wrappedClass: Class[_]

    def initialize(node: ObjectSyncNodeImpl[A]): Unit = {
        if (this.node != null)
            throw new SyncObjectAlreadyInitialisedException(s"This synchronized object is already initialized !")
        //if (location != null && location != node.reference)
        //    throw new IllegalArgumentException(s"Synchronized Object Network Reference of given node mismatches from the actual object's location ($location vs ${node.reference})")
        val puppeteer = node.puppeteer
        this.location = node.reference
        this.puppeteer = node.puppeteer
        this.contract = node.contract
        this.presenceOnNetwork = node.objectPresence
        this.node = node
        this.choreographer = new InvocationChoreographer()

        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
        this.isNotMirroring = !(!isOrigin && contract.remoteObjectInfo.isDefined)
    }

    override def isOrigin: Boolean = currentIdentifier == ownerID

    override def isMirroring: Boolean = !isNotMirroring

    override def reference: SyncObjectReference = location

    override def presence: NetworkObjectPresence = presenceOnNetwork

    override def getSourceClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getNode: ObjectSyncNode[A] = node

    protected def handleCall[R](id: Int)(args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        if (!isInitialized) {
            //throw new IllegalStateException(s"Synchronized object at '${location}' is not initialised")
            return superCall(args).asInstanceOf[R]
        }
        val methodContract   = contract.getMethodContract(id)
        //Arguments that must be synchronized wil be synchronized according to method contract.
        val synchronizedArgs = methodContract.synchronizeArguments(args, puppeteer.synchronizedObj(_))
        //println(s"Method name = ${methodBehavior.desc.javaMethod.getName}")
        if (!methodContract.isRMIActivated || choreographer.isMethodExecutionForcedToLocal) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }
        val data = new methodContract.RemoteInvocationExecution {
            override val syncObject: SynchronizedObject[_] = AbstractSynchronizedObject.this
            override val arguments : Array[Any]            = synchronizedArgs
            override val puppeteer : Puppeteer[_]          = AbstractSynchronizedObject.this.puppeteer

            override def doSuperCall(): Any = {
                if (isNotMirroring)
                    superCall(synchronizedArgs)
                else throw new MirroringObjectInvocationException("")

            }
        }
        methodContract.executeRemoteMethodInvocation(data)
    }

    private def getMirroringCallExceptionMessage: String = {
        s"Attempted to call a method on a distant object representation. This object is mirroring $reference on engine ${ownerID}"
    }

    @inline override def isInitialized: Boolean = puppeteer != null

}
