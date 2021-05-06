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

package fr.linkit.api.connection.network.cache.repo

import fr.linkit.api.connection.network.cache.repo.generation.PuppeteerDescription

trait Puppeteer[S <: Serializable] {

    val puppeteerDescription: PuppeteerDescription

    def getPuppet: S

    def getPuppetWrapper: S with PuppetWrapper[S]

    def sendInvokeAndWaitResult[R](methodName: String, args: Array[Any]): R

    def sendPuppetUpdate(newVersion: S): Unit

    def init(wrapper: S with PuppetWrapper[S], puppet: S): Unit

    def sendInvoke(methodId: Int, args: Array[Any]): Unit

    def sendFieldUpdate(fieldId: Int, newValue: Any): Unit
}
