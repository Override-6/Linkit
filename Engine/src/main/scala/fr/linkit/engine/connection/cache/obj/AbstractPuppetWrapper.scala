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

import fr.linkit.api.connection.cache.obj.behavior.{MethodBehavior, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.WrapperMethodInvocation
import fr.linkit.api.connection.cache.obj.{InvocationChoreographer, PuppetWrapper, Puppeteer}
import fr.linkit.engine.connection.cache.obj.generation.WrapperInstantiationHelper
import fr.linkit.engine.connection.cache.obj.invokation.{AbstractWrapperMethodInvocation, SimpleRMIRulesAgreement}

trait AbstractPuppetWrapper[A <: AnyRef] extends PuppetWrapper[A] { wrapper =>

    @transient protected var puppeteer           : Puppeteer[A]            = _
    @transient protected var behavior            : WrapperBehavior[A]      = _
    @transient protected var choreographer       : InvocationChoreographer = _
    protected            var puppeteerDescription: WrapperNodeInfo         = _

    private var currentIdentifier: String = _
    private var ownerID          : String = _

    def wrappedClass: Class[_]

    override def initPuppeteer(puppeteer: Puppeteer[A]): Unit = {
        if (this.puppeteer != null)
            throw new PuppetAlreadyInitialisedException(s"This puppet is already initialized ! ($puppeteer)")
        this.puppeteer = puppeteer
        this.puppeteerDescription = puppeteer.puppeteerInfo
        this.behavior = puppeteer.wrapperBehavior
        this.puppeteer.init(asAutoWrapped)
        this.choreographer = new InvocationChoreographer() //TODO Have the same choreographer for the entire tree
        this.currentIdentifier = puppeteer.currentIdentifier
        this.ownerID = puppeteer.ownerID
    }

    @inline override def isInitialized: Boolean = puppeteer != null

    override def detachedClone: A = {
        WrapperInstantiationHelper.detachedWrapperClone(this)._1
    }

    override def getWrappedClass: Class[A] = wrappedClass.asInstanceOf[Class[A]]

    override def asWrapped(): A = asAutoWrapped

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getBehavior: WrapperBehavior[A] = behavior

    override def getNodeInfo: WrapperNodeInfo = puppeteerDescription

    private def synchronizedParams(bhv: MethodBehavior, objects: Array[Any]): Array[Any] = {
        var i                  = -1
        val synchronizedParams = bhv.synchronizedParams
        val pup                = puppeteer
        objects.map(obj => {
            i += 1
            if (!synchronizedParams(i) || obj.isInstanceOf[PuppetWrapper[_]])
                obj
            else obj match {
                case anyRef: AnyRef => pup.synchronizedObj(anyRef)
                case _              =>
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
        if (choreographer.isMethodExecutionForcedToLocal) {
            return superCall(synchronizedArgs).asInstanceOf[R]
        }
        if (!methodBehavior.isRMIEnabled) {
            return performSuperCall(superCall(synchronizedArgs))
        }

        val invocation = new AbstractWrapperMethodInvocation[R](methodBehavior, this) {
            override val callerIdentifier : String     = AbstractPuppetWrapper.this.currentIdentifier
            override val currentIdentifier: String     = AbstractPuppetWrapper.this.currentIdentifier
            override val methodArguments  : Array[Any] = synchronizedArgs

            override def callSuper(): R = {
                performSuperCall[R](superCall(synchronizedArgs))
            }
        }
        val agreement  = createAgreement(invocation)

        methodBehavior.handler.handleRMI[R](agreement, invocation)
    }

    private def asAutoWrapped: A with PuppetWrapper[A] = this.asInstanceOf[A with PuppetWrapper[A]]

    private def createAgreement(invocation: WrapperMethodInvocation[_]): SimpleRMIRulesAgreement = {
        val agreement = new SimpleRMIRulesAgreement(currentIdentifier, ownerID)
        val behavior  = invocation.methodBehavior
        behavior.completeAgreement(agreement, invocation)
        agreement
    }

    @inline private def performSuperCall[R](@inline superCall: => Any): R = {
        choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        }
    }
}
