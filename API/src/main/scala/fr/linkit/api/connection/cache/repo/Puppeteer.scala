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

package fr.linkit.api.connection.cache.repo

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.generation.PuppeteerDescription

trait Puppeteer[S] {
    
    val ownerID: String

    val repo: ObjectSynchronizer

    val puppeteerDescription: PuppeteerDescription

    val puppetDescription: PuppetDescription[S]

    def isCurrentEngineOwner: Boolean

    def getPuppetWrapper: S with PuppetWrapper[S]

    def sendInvokeAndWaitResult[R](methodId: Int, args: Array[Array[Any]]): R

    def sendPuppetUpdate(newVersion: S): Unit

    def init(wrapper: S with PuppetWrapper[S]): Unit

    def sendInvoke(methodId: Int, args: Array[Array[Any]]): Unit

    def sendFieldUpdate(fieldId: Int, newValue: Any): Unit
}
