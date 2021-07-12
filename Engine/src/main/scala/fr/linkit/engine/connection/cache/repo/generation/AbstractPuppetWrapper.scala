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

package fr.linkit.engine.connection.cache.repo.generation
import fr.linkit.api.connection.cache.repo.description.{PuppetDescription, PuppeteerDescription}
import fr.linkit.api.connection.cache.repo.{InvocationChoreographer, PuppetWrapper, Puppeteer}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.repo.PuppetAlreadyInitialisedException

trait AbstractPuppetWrapper[A] extends PuppetWrapper[A] {
    @transient protected var puppeteer: Puppeteer[A] = _
    @transient protected var description: PuppetDescription[A] = _
    @transient protected var choreographer: InvocationChoreographer = _
    protected var puppeteerDescription: PuppeteerDescription = _

    private def asWrapper: A with PuppetWrapper[A] = this.asInstanceOf[A with PuppetWrapper[A]]

    override def initPuppeteer(puppeteer: Puppeteer[A]): Unit = {
        if (this.description != null)
            throw new PuppetAlreadyInitialisedException("This puppet is already initialized !")
        this.puppeteer = puppeteer
        this.puppeteerDescription = puppeteer.puppeteerDescription
        this.description = puppeteer.puppetDescription
        this.puppeteer.init(asWrapper)
        this.choreographer = new InvocationChoreographer()
    }

    override def isInitialized: Boolean = puppeteer != null

    override def detachedSnapshot: A = {
        WrapperInstantiator.detachedClone(this)
    }

    override def asWrapped(): A = asWrapper

    override def getChoreographer: InvocationChoreographer = choreographer

    override def getPuppeteer: Puppeteer[A] = puppeteer

    override def getPuppetDescription: PuppetDescription[A] = description

    override def getPuppeteerDescription: PuppeteerDescription = puppeteerDescription

    protected def handleCall[R](id: Int, defaultReturnValue: R, invokeOnlyResult: R)
                                     (args: Array[Array[Any]])(superCall: => Any = null): R = {
        lazy val name = description.getMethodDesc(id).get.javaMethod.getName
        lazy val argsString = args.map(_.mkString("(", ", ", ")")).mkString("")
        if (choreographer.isMethodExecutionForcedToLocal) {
            println(s"forced local method call $name$argsString.")
            return superCall.asInstanceOf[R]
        }
        AppLogger.vDebug(s"$name: Performing rmi call for $name$argsString (id: $id)")
        if (!description.isRMIEnabled(id)) {
            AppLogger.vDebug("The call is redirected to current object...")
            return performSuperCall(superCall)
        }
        // From here we are sure that we want to perform a remote
        // method invocation. (A Local invocation (super.xxx()) can be added).
        if (description.isInvokeOnly(id)) {
            AppLogger.vDebug("The call is redirected to current object...")
            puppeteer.sendInvoke(id, args)
            var localResult: R = defaultReturnValue
            if (description.isLocalInvocationForced(id)) {
                localResult = performSuperCall(superCall)
            }
            AppLogger.vDebug("Returned local result = " + localResult)
            //# Note1: value of 'InvokeOnlyResult' can be "localResult", which will return the variable.
            //# Note2: Be aware that you can get a null value returned
            //#        if the 'InvokeOnlyResult' value of the annotated
            //#        method is set to "localResult" and the invocation
            //#        kind does not force local invocation.
            return localResult
        }
        var result: Any = defaultReturnValue
        if (description.isLocalInvocationForced(id)) {
            AppLogger.vDebug("The method invocation is redirected to current object...")
            result = performSuperCall(superCall)
            AppLogger.vDebug("Also performing asynchronous remote method invocation...")
            puppeteer.sendInvoke(id, args)
        } else {
            AppLogger.vDebug("Performing synchronous remote method invocation...")
            result = puppeteer.sendInvokeAndWaitResult[R](id, args)
        }
        AppLogger.vDebug("Returned rmi result = " + result)
        result.asInstanceOf[R]
    }

    @inline private def performSuperCall[R](@inline superCall: => Any): R = {
        choreographer.forceLocalInvocation[R] {
            superCall.asInstanceOf[R]
        }
    }

    class Test[S >: String] extends AbstractPuppetWrapper[Test[S]] {
        override def getWrappedClass: Class[_] = classOf[Test[_]].getSuperclass
    }
}
