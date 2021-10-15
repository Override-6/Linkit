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

import fr.linkit.api.gnom.cache.sync.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.gnom.cache.sync.behavior.{ObjectBehavior, ObjectBehaviorStore}
import fr.linkit.api.gnom.cache.sync.invokation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invokation.local.CallableLocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{SyncNode, SyncObjectReference}
import fr.linkit.api.gnom.cache.sync.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    @transient final protected var location         : SyncObjectReference     = _
    @transient final protected var puppeteer        : Puppeteer[A]            = _
    @transient final protected var behavior         : ObjectBehavior[A]       = _
    @transient final protected var choreographer    : InvocationChoreographer = _
    @transient final protected var store            : ObjectBehaviorStore     = _
    @transient private         var presenceOnNetwork: NetworkObjectPresence   = _
    @transient private         var node             : SyncNode[A]             = _
    //fast cache for handleCall
    @transient private         var currentIdentifier: String                  = _
    @transient private         var ownerID          : String                  = _

    def wrappedClass: Class[_]

    override def initialize(node: SyncNode[A]): Unit = {
        if (this.puppeteer != null)
            throw new SyncObjectAlreadyInitialisedException(s"This puppet is already initialized !")
        val puppeteer = node.puppeteer
        this.store = node.tree.behaviorStore
        this.puppeteer = node.puppeteer
        this.location = node.reference
        this.behavior = puppeteer.objectBehavior
        this.presenceOnNetwork = node.objectPresence
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
        this.node = node
        this.choreographer = new InvocationChoreographer()
    }

    @transient override def isOwnedByCurrent: Boolean = currentIdentifier == ownerID

    override final def reference: SyncObjectReference = location

    override def presence: NetworkObjectPresence = presenceOnNetwork

    override def getSuperClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getStore: ObjectBehaviorStore = store

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: ObjectBehavior[A] = behavior

    //private def asAutoSync: A with SynchronizedObject[A] = this.asInstanceOf[A with SynchronizedObject[A]]

    protected def handleCall[R](id: Int)(args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        if (!isInitialized)
            return superCall(args).asInstanceOf[R]
        val methodBhv        = behavior.getMethodBehavior(id).get
        val synchronizedArgs = synchronizedParams(methodBhv, args)
        //println(s"Method name = ${methodBehavior.desc.javaMethod.getName}")
        if (choreographer.isMethodExecutionForcedToLocal || !methodBhv.isActivated) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }

        val localInvocation: CallableLocalMethodInvocation[R] = new AbstractMethodInvocation[R](methodBhv, this) with CallableLocalMethodInvocation[R] {
            override val methodArguments: Array[Any]             = synchronizedArgs
            override val methodBehavior : InternalMethodBehavior = methodBhv

            override def callSuper(): R = {
                val params = modifiedParamsForLocal(methodBhv, synchronizedArgs)
                performSuperCall[R](!methodBhv.innerInvocations, superCall(params))
            }
        }

        methodBhv.handler.handleRMI[R](localInvocation)
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    private def synchronizedParams(bhv: MethodBehavior, objects: Array[Any]): Array[Any] = {
        var i              = -1
        val paramBehaviors = bhv.parameterBehaviors
        val pup            = puppeteer
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

    private def modifiedParamsForLocal(bhv: MethodBehavior, args: Array[Any]): Array[Any] = {
        var i              = -1
        val paramBehaviors = bhv.parameterBehaviors
        args.map {
            case ref: AnyRef => //will never crash (primitives are wrapped by objects)
                i += 1
                store.modifyParameterForLocalComingFromLocal(args, ref, paramBehaviors(i))
        }
    }

    @inline private def performSuperCall[R](forceLocal: Boolean, @inline superCall: => Any): R = {
        if (forceLocal) choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        } else {
            superCall.asInstanceOf[R]
        }
    }
}
