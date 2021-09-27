/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.obj

import fr.linkit.api.connection.cache.obj.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.connection.cache.obj.behavior.{ObjectBehavior, ObjectBehaviorStore}
import fr.linkit.api.connection.cache.obj.invokation.InvocationChoreographer
import fr.linkit.api.connection.cache.obj.invokation.local.CallableLocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.cache.obj.tree.{SyncNode, SyncNodeLocation}
import fr.linkit.api.connection.cache.obj.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.api.connection.reference.presence.ObjectNetworkPresence
import fr.linkit.engine.connection.cache.obj.invokation.AbstractMethodInvocation

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    @transient final protected var location         : SyncNodeLocation        = _
    @transient final protected var puppeteer        : Puppeteer[A]            = _
    @transient final protected var behavior         : ObjectBehavior[A]       = _
    @transient final protected var choreographer    : InvocationChoreographer = _
    @transient final protected var store            : ObjectBehaviorStore     = _
    @transient private         var presenceOnNetwork: ObjectNetworkPresence   = _
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
        this.location = node.location
        this.behavior = puppeteer.objectBehavior
        this.choreographer = new InvocationChoreographer()
        this.presenceOnNetwork = node.objectPresence
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
    }

    @transient override def isOwnedByCurrent: Boolean = currentIdentifier == ownerID

    override def getPresenceOnNetwork: ObjectNetworkPresence = presenceOnNetwork

    override def getLocation: SyncNodeLocation = location

    override def getSuperClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def asWrapped(): A = asAutoSync

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getStore: ObjectBehaviorStore = store

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: ObjectBehavior[A] = behavior

    private def asAutoSync: A with SynchronizedObject[A] = this.asInstanceOf[A with SynchronizedObject[A]]

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
