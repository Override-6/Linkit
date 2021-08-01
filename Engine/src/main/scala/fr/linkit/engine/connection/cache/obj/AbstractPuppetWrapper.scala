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

import fr.linkit.api.connection.cache.obj.description.{MethodBehavior, WrapperNodeInfo, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.{InvocationChoreographer, PuppetWrapper, Puppeteer}
import fr.linkit.engine.connection.cache.obj.generation.WrapperInstantiationHelper

trait AbstractPuppetWrapper[A <: AnyRef] extends PuppetWrapper[A] {

    @transient protected var puppeteer           : Puppeteer[A]            = _
    @transient protected var behavior            : WrapperBehavior[A]      = _
    @transient protected var choreographer       : InvocationChoreographer = _
    protected            var puppeteerDescription: WrapperNodeInfo         = _

    def wrappedClass: Class[_]

    override def initPuppeteer(puppeteer: Puppeteer[A]): Unit = {
        if (this.puppeteer != null)
            throw new PuppetAlreadyInitialisedException(s"This puppet is already initialized ! ($puppeteer)")
        this.puppeteer = puppeteer
        this.puppeteerDescription = puppeteer.puppeteerInfo
        this.behavior = puppeteer.wrapperBehavior
        this.puppeteer.init(asWrapper)
        this.choreographer = new InvocationChoreographer() //TODO Have the same for the entire tree
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    override def detachedClone: A = {
        WrapperInstantiationHelper.detachedWrapperClone(this)._1
    }

    override def getWrappedClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def asWrapped(): A = asWrapper

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: WrapperBehavior[A] = behavior

    override def getNodeInfo: WrapperNodeInfo = puppeteerDescription

    private def synchronizedParams(bhv: MethodBehavior, objects: Array[Any]): Array[Any] = {
        var i = -1
        val synchronizedParams = bhv.synchronizedParams
        val pup = puppeteer
        objects.map(obj => {
            i += 1
            if (!synchronizedParams(i) || obj.isInstanceOf[PuppetWrapper[_]])
                obj
            else pup.synchronizedObj(obj)
        })
    }

    protected def handleCall[R](id: Int, defaultReturnValue: R)
                               (args: Array[Any])(superCall: Array[Any] => Any): R = {
        //if (!isInitialized) //May be here only during tests
        //    return superCall(args).asInstanceOf[R]
        val methodBehavior   = behavior.getMethodBehavior(id).get
        val synchronizedArgs = synchronizedParams(methodBehavior, args)
        if (choreographer.isMethodExecutionForcedToLocal) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }
        if (!methodBehavior.isRMIEnabled) {
            return performSuperCall(superCall(synchronizedArgs))
        }
        methodBehavior.handler
            .handleRMI[R](this)(id, defaultReturnValue)(synchronizedArgs)(performSuperCall[R](superCall(synchronizedArgs)))
    }

    private def asWrapper: A with PuppetWrapper[A] = this.asInstanceOf[A with PuppetWrapper[A]]

    @inline private def performSuperCall[R](@inline superCall: => Any): R = {
        choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        }
    }
}
