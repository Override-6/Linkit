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
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.InvocationChoreographer
import fr.linkit.api.connection.cache.obj.invokation.local.CallableLocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.cache.obj.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.engine.connection.cache.obj.generation.SyncObjectInstantiationHelper
import fr.linkit.engine.connection.cache.obj.invokation.AbstractMethodInvocation

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    @transient final protected var puppeteer           : Puppeteer[A]            = _
    @transient final protected var behavior            : ObjectBehavior[A]       = _
    @transient final protected var choreographer       : InvocationChoreographer = _
    @transient final protected var store               : ObjectBehaviorStore     = _
    //fast cache for handleCall
    @transient private         var currentIdentifier   : String                  = _
    @transient private         var ownerID: String = _
    @transient protected       var nodeInfo: SyncNodeInfo            = _

    def wrappedClass: Class[_]

    override def initPuppeteer(puppeteer: Puppeteer[A], store: ObjectBehaviorStore): Unit = {
        if (this.puppeteer != null)
            throw new SyncObjectAlreadyInitialisedException(s"This puppet is already initialized ! ($puppeteer)")
        this.store = store
        this.puppeteer = puppeteer
        this.nodeInfo = puppeteer.nodeInfo
        this.behavior = puppeteer.wrapperBehavior
        this.puppeteer.init(asAutoWrapped)
        this.choreographer = new InvocationChoreographer()
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    @transient override def isOwnedByCurrent: Boolean = currentIdentifier == ownerID

    override def detachedClone: A = {
        SyncObjectInstantiationHelper.detachedWrapperClone(this, puppeteer.cache.network.refStore)._1
    }

    override def getSuperClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def asWrapped(): A = asAutoWrapped

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getStore: ObjectBehaviorStore = store

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: ObjectBehavior[A] = behavior

    override def getNodeInfo: SyncNodeInfo = nodeInfo

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

    protected def handleCall[R](id: Int)(args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        //if (!isInitialized) //May be here only during tests
        //    return superCall(args).asInstanceOf[R]
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
                performSuperCall[R](!methodBhv.innerInvocations, superCall(modifiedParamsForLocal(methodBhv, synchronizedArgs)))
            }
        }

        methodBhv.handler.handleRMI[R](localInvocation)
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

    private def asAutoWrapped: A with SynchronizedObject[A] = this.asInstanceOf[A with SynchronizedObject[A]]

    @inline private def performSuperCall[R](forceLocal: Boolean, @inline superCall: => Any): R = {
        if (forceLocal) choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        } else {
            superCall.asInstanceOf[R]
        }
    }
}
