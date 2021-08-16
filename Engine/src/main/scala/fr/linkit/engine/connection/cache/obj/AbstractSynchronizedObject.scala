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

import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.MethodBehavior
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.InvocationChoreographer
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.cache.obj.{SyncObjectAlreadyInitialisedException, SynchronizedObject}
import fr.linkit.engine.connection.cache.obj.generation.SyncObjectInstantiationHelper
import fr.linkit.engine.connection.cache.obj.invokation.AbstractSynchronizedMethodInvocation

trait AbstractSynchronizedObject[A <: AnyRef] extends SynchronizedObject[A] {

    @transient final protected var puppeteer           : Puppeteer[A]                  = _
    @transient final protected var behavior            : SynchronizedObjectBehavior[A] = _
    @transient final protected var choreographer       : InvocationChoreographer       = _
    protected final            var puppeteerDescription: SyncNodeInfo                  = _

    private var currentIdentifier: String = _
    private var ownerID          : String = _

    def wrappedClass: Class[_]

    override def initPuppeteer(puppeteer: Puppeteer[A]): Unit = {
        if (this.puppeteer != null)
            throw new SyncObjectAlreadyInitialisedException(s"This puppet is already initialized ! ($puppeteer)")
        this.puppeteer = puppeteer
        this.puppeteerDescription = puppeteer.nodeInfo
        this.behavior = puppeteer.wrapperBehavior
        this.puppeteer.init(asAutoWrapped)
        this.choreographer = new InvocationChoreographer()
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    override def isOwnedByCurrent: Boolean = currentIdentifier == ownerID

    override def detachedClone: A = {
        SyncObjectInstantiationHelper.detachedWrapperClone(this)._1
    }

    override def getSuperClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def asWrapped(): A = asAutoWrapped

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: SynchronizedObjectBehavior[A] = behavior

    override def getNodeInfo: SyncNodeInfo = puppeteerDescription

    private def synchronizedParams(bhv: MethodBehavior, objects: Array[Any]): Array[Any] = {
        var i              = -1
        val paramBehaviors = bhv.parameterBehaviors
        val pup            = puppeteer
        objects.map(obj => {
            i += 1
            val behavior = paramBehaviors(i)
            val modifier = behavior.modifier
            if (modifier != null) modifier(obj) else obj match {
                case sync: SynchronizedObject[_] => sync
                case anyRef: AnyRef              => pup.synchronizedObj(anyRef)
                case other                       => other
            }
        })
    }

    protected def handleCall[R](id: Int)
                               (args: Array[Any])(superCall: Array[Any] => Any = null): R = {
        //if (!isInitialized) //May be here only during tests
        //    return superCall(args).asInstanceOf[R]
        val methodBehavior   = behavior.getMethodBehavior(id).get
        val synchronizedArgs = synchronizedParams(methodBehavior, args)
        //println(s"Method name = ${methodBehavior.desc.javaMethod.getName}")
        if (choreographer.isMethodExecutionForcedToLocal || !methodBehavior.isRMIEnabled) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }

        val invocation = new AbstractSynchronizedMethodInvocation[R](methodBehavior, this) {
            override val callerIdentifier : String     = AbstractSynchronizedObject.this.currentIdentifier
            override val currentIdentifier: String     = AbstractSynchronizedObject.this.currentIdentifier
            override val methodArguments  : Array[Any] = synchronizedArgs

            override def callSuper(): R = {
                performSuperCall[R](superCall(synchronizedArgs))
            }
        }

        methodBehavior.handler.handleRMI[R](invocation)
    }

    private def asAutoWrapped: A with SynchronizedObject[A] = this.asInstanceOf[A with SynchronizedObject[A]]

    @inline private def performSuperCall[R](@inline superCall: => Any): R = {
        choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        }
    }
}
