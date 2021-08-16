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

package fr.linkit.api.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, SynchronizedObjectBehavior}
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.{SynchronizedObject, SynchronizedObjectCenter}

import java.util.concurrent.ThreadLocalRandom

/**
 * The puppeteer of a SynchronizedObject creates all RMI requests and handles the results.
 * */
trait Puppeteer[S <: AnyRef] {

    /**
     * The synchronized object's node informations.
     */
    val nodeInfo: SyncNodeInfo
    /**
     * The engine's identifier that have created the synchronized object
     * */
    val ownerID: String = nodeInfo.owner

    /**
     * The object center that stores the synchronized object.
     */
    val center: SynchronizedObjectCenter[_]

    /**
     * The behavior of the synchronized object.
     * */
    val wrapperBehavior: SynchronizedObjectBehavior[S]

    /**
     * The identifier of the current engine.
     * */
    val currentIdentifier: String

    /**
     * @return true if the current engine have created the synchronized object.
     * */
    def isCurrentEngineOwner: Boolean = currentIdentifier == ownerID

    /**
     * @return the synchronized object.
     * */
    def getSynchronizedObject: S with SynchronizedObject[S]

    /**
     * Send an RMI invocation based on the given agreement and invocation and waits for any result (return value or exception)
     * @throws RMIException if any exception was received from the RMI response.
     * @param agreement the agreement that determines who will handle the request, who will be listened for the invocation result.
     * @param invocation the method's invocation information.
     * @tparam R the return type of the RMI result value.
     * @return the RMI result value
     */
    def sendInvokeAndWaitResult[R](agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[R]): R //TODO add a timeout. (here or in the MethodBehavior)

    /**
     * Send an RMI Invocation based on the given agreement and invocation without waiting for any result.
     * @param agreement the agreement that determines who will handle the request, who will be listened for the invocation result.
     * @param invocation the method's invocation information.
     */
    def sendInvoke(agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[_]): Unit

    //TODO make this and "init" for internal use only
    def synchronizedObj(obj: AnyRef, id: Int = ThreadLocalRandom.current().nextInt()): AnyRef

    def init(wrapper: S with SynchronizedObject[S]): Unit

}
