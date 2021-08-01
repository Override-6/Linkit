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

import fr.linkit.api.connection.cache.obj.description.{WrapperNodeInfo, WrapperBehavior}
import java.util.concurrent.ThreadLocalRandom

import fr.linkit.api.connection.cache.obj.tree.{SyncNode, SynchronizedObjectTree}

trait Puppeteer[S <: AnyRef] {

    val ownerID: String

    val center: SynchronizedObjectCenter[_]

    val puppeteerInfo: WrapperNodeInfo

    val wrapperBehavior: WrapperBehavior[S]

    def isCurrentEngineOwner: Boolean

    def getPuppetWrapper: S with PuppetWrapper[S]

    def sendInvokeAndWaitResult[R](methodId: Int, args: Array[Any]): R

    def init(wrapper: S with PuppetWrapper[S]): Unit

    def sendInvoke(methodId: Int, args: Array[Any]): Unit

    //TODO make this for internal use only
    def synchronizedObj(obj: AnyRef, id: Int = ThreadLocalRandom.current().nextInt()): AnyRef
}
