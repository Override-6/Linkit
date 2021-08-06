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

package fr.linkit.api.connection.cache.obj

import fr.linkit.api.connection.cache.obj.behavior.{MethodBehavior, RMIRulesAgreement, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.WrapperMethodInvocation

import java.util.concurrent.ThreadLocalRandom

trait Puppeteer[S <: AnyRef] {

    val ownerID: String

    val center: SynchronizedObjectCenter[_]

    val puppeteerInfo: WrapperNodeInfo

    val wrapperBehavior: WrapperBehavior[S]

    val currentIdentifier: String

    def isCurrentEngineOwner: Boolean

    def getPuppetWrapper: S with PuppetWrapper[S]

    def sendInvokeAndWaitResult[R](agreement: RMIRulesAgreement, invocation: WrapperMethodInvocation[R]): R

    def sendInvoke(agreement: RMIRulesAgreement, invocation: WrapperMethodInvocation[_]): Unit

    //TODO make this and "init" for internal use only
    def synchronizedObj(obj: AnyRef, id: Int = ThreadLocalRandom.current().nextInt()): AnyRef

    def init(wrapper: S with PuppetWrapper[S]): Unit

}
