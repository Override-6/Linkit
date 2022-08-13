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

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.invocation.{InvocationChoreographer, MirroringObjectInvocationException}
import fr.linkit.api.gnom.cache.sync.tree.{ObjectConnector, ObjectSyncNode}
import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObjectAlreadyInitialisedException, ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.node.ObjectSyncNodeImpl

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {
    
    protected final          var location         : ConnectedObjectReference = _
    protected final          var isMirrored0      : Boolean                  = _
    @transient private final var puppeteer        : Puppeteer[A]             = _
    @transient private final var contract         : StructureContract[A]     = _
    @transient private final var choreographer    : InvocationChoreographer  = _
    @transient private final var presenceOnNetwork: NetworkObjectPresence    = _
    @transient private final var node             : ObjectSyncNode[A]        = _
    @transient private final var connector        : ObjectConnector          = _
    
    //cached values for handleCall
    @transient private final var currentIdentifier: String  = _
    @transient private final var ownerID          : String  = _
    @transient private final var isNotMirroring   : Boolean = _
    
    def originClass: Class[_]
    
    def initialize(node: ObjectSyncNodeImpl[A]): Unit = {
        if (isInitialized)
            throw new ConnectedObjectAlreadyInitialisedException(s"This synchronized object is already initialized !")
        //if (location != null && location != node.reference)
        //    throw new IllegalArgumentException(s"Synchronized Object Network Reference of given node mismatches from the actual object's location ($location vs ${node.reference})")
        val puppeteer = node.puppeteer
        this.location = node.reference
        this.isMirrored0 = node.isMirrored
        
        this.puppeteer = node.puppeteer
        this.contract = node.contract
        this.presenceOnNetwork = node.objectPresence
        this.node = node
        this.choreographer = node.choreographer
        this.connector = node.tree
        
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
        this.isNotMirroring = !node.isMirroring
    }
    
    override def isOrigin: Boolean = currentIdentifier == ownerID
    
    override def isMirroring: Boolean = !isNotMirroring
    
    override def isMirrored: Boolean = isMirrored0
    
    override def reference: ConnectedObjectReference = location
    
    override def presence: NetworkObjectPresence = presenceOnNetwork
    
    override def getSourceClass: Class[A] = originClass.asInstanceOf[Class[A]]
    
    override def getChoreographer: InvocationChoreographer = choreographer
    
    override def getPuppeteer: Puppeteer[A] = puppeteer
    
    override def getNode: ObjectSyncNode[A] = node
    
    @transient private lazy val classDef = {
        var interfaces = getClass.getInterfaces.tail
        val mainClass  = {
            val superCl = getClass.getSuperclass
            if (interfaces.nonEmpty && (superCl == classOf[Object] || superCl == null)) {
                val itf = interfaces.head
                interfaces = interfaces.tail
                itf
            } else superCl
        }
        SyncClassDef(mainClass, interfaces)
    }
    
    override def getClassDef: SyncClassDef = classDef
    
    protected final def handleCall[R](id: Int)(args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        if (!isInitialized) {
            //throw new IllegalStateException(s"Synchronized object at '${location}' is not initialised")
            return superCall(args).asInstanceOf[R]
        }
        val methodContract = {
            val opt = contract.findMethodContract(id)
            if (opt.isEmpty) { //no contract specified so we just execute the method.
                return superCall(args).asInstanceOf[R]
            }
            opt.get
        }
        val choreographer  = methodContract.choreographer
        //Arguments that must be synchronized wil be synchronized according to method contract.
        methodContract.connectArgs(args, connector.createConnectedObj(location))
        //println(s"Method name = ${methodBehavior.desc.javaMethod.getName}")
        if (!methodContract.isRMIActivated || choreographer.isMethodExecutionForcedToLocal) {
            return methodContract.applyReturnValue(superCall(args), connector.createConnectedObj(location)).asInstanceOf[R]
        }
        val data = new methodContract.RemoteInvocationExecution {
            override val obj      : ChippedObject[_] = AbstractSynchronizedObject.this
            override val connector: ObjectConnector  = AbstractSynchronizedObject.this.connector
            override val puppeteer: Puppeteer[_]     = AbstractSynchronizedObject.this.puppeteer
            override val arguments: Array[Any]       = args
            
            override def doSuperCall(): Any = {
                if (isNotMirroring) {
                    superCall(args)
                } else throw new MirroringObjectInvocationException(getMirroringCallExceptionMessage)
            }
        }
        methodContract.executeRemoteMethodInvocation(data)
    }
    
    private def getMirroringCallExceptionMessage: String = {
        s"Attempted to call a method on a distant object representation. This object is mirroring $reference on engine ${ownerID}"
    }
    
    @inline override def isInitialized: Boolean = node != null
    
}
