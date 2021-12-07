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

import fr.linkit.api.gnom.cache.sync.contract.SynchronizedStructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.UsageMethodBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.{SynchronizedObjectContractFactory, SynchronizedStructureBehavior}
import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invokation.local.CallableLocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference}
import fr.linkit.api.gnom.cache.sync.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    protected final          var location         : SyncObjectReference               = _
    @transient final         var puppeteer        : Puppeteer[A]                      = _
    @transient final         var contract         : SynchronizedStructureContract[A]  = _
    @transient final         var choreographer    : InvocationChoreographer           = _
    @transient final         var behaviorFactory  : SynchronizedObjectContractFactory = _
    @transient private final var presenceOnNetwork: NetworkObjectPresence             = _
    @transient private final var node             : SyncNode[A]                       = _

    //cached for handleCall
    @transient private final var currentIdentifier: String                           = _
    @transient private final var ownerID          : String                           = _
    @transient final         var behavior         : SynchronizedStructureBehavior[A] = _

    def wrappedClass: Class[_]

    override def initialize(node: SyncNode[A]): Unit = {
        if (this.node != null)
            throw new SyncObjectAlreadyInitialisedException(s"This synchronized object is already initialized !")
        //if (location != null && location != node.reference)
        //    throw new IllegalArgumentException(s"Synchronized Object Network Reference of given node mismatches from the actual object's location ($location vs ${node.reference})")
        this.location = node.reference
        val puppeteer = node.puppeteer
        this.behaviorFactory = node.tree.behaviorFactory
        this.puppeteer = node.puppeteer
        this.contract = node.contract
        this.behavior = contract.behavior
        this.presenceOnNetwork = node.objectPresence
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
        this.node = node
        this.choreographer = new InvocationChoreographer()
    }

    @transient override def isOwnedByCurrent: Boolean = currentIdentifier == ownerID

    override def getBehaviorFactory: SynchronizedObjectContractFactory = behaviorFactory

    override final def reference: SyncObjectReference = location

    override def presence: NetworkObjectPresence = presenceOnNetwork

    override def getSuperClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getNode: SyncNode[A] = node

    override def getContract: SynchronizedStructureContract[A] = contract

    //private def asAutoSync: A with SynchronizedObject[A] = this.asInstanceOf[A with SynchronizedObject[A]]

    protected def handleCall[R](id: Int)(args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        if (!isInitialized) {
            //throw new IllegalStateException(s"Synchronized object at '${location}' is not initialised")
            return superCall(args).asInstanceOf[R]
        }
        val methodContract   = contract.getMethodContract(id).get
        val methodBhv        = methodContract.behavior
        val synchronizedArgs = synchronizedParams(methodBhv, args)
        //println(s"Method name = ${methodBehavior.desc.javaMethod.getName}")
        if (!methodBhv.isActivated || choreographer.isMethodExecutionForcedToLocal) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }

        val localInvocation: CallableLocalMethodInvocation[R] = new AbstractMethodInvocation[R](methodContract, node) with CallableLocalMethodInvocation[R] {
            override val methodArguments: Array[Any] = synchronizedArgs

            override def callSuper(): R = {
                performSuperCall[R](methodBhv.forceLocalInnerInvocations, superCall(synchronizedArgs))
            }
        }

        methodContract.handler.handleRMI[R](localInvocation)
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    private def synchronizedParams(bhv: UsageMethodBehavior, objects: Array[Any]): Array[Any] = {
        if (bhv eq null)
            return objects
        val paramBehaviors = bhv.parameterBehaviors
        if (paramBehaviors.isEmpty)
            return objects
        var i   = -1
        val pup = puppeteer
        objects.map(param => {
            i += 1
            val behavior = paramBehaviors(i)
            param match {
                case sync: SynchronizedObject[_]            => sync
                case anyRef: AnyRef if behavior.isActivated => pup.synchronizedObj(anyRef)
                case other                                  => other
            }
        })
    }

    @inline private def performSuperCall[R](forceLocal: Boolean, @inline superCall: => Any): R = {
        if (forceLocal) choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        } else {
            superCall.asInstanceOf[R]
        }
    }
}
