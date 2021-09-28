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

package fr.linkit.api.gnom.cache.sync.invokation.remote

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehavior
import fr.linkit.api.gnom.cache.sync.tree.SyncNodeLocation
import fr.linkit.api.application.network.Network

import java.util.concurrent.ThreadLocalRandom

/**
 * The puppeteer of a SynchronizedObject creates all RMI requests and handles the results.
 * */
trait Puppeteer[S <: AnyRef] {

    /**
     * The synchronized object's node informations.
     */
    val nodeLocation: SyncNodeLocation
    /**
     * The engine's identifier that have created the synchronized object
     * */
    val ownerID: String = nodeLocation.owner

    /**
     * The object center that stores the synchronized object.
     */
    val cache: SynchronizedObjectCache[_]

    /**
     * The behavior of the synchronized object.
     * */
    val objectBehavior: ObjectBehavior[S]

    /**
     * The identifier of the current engine.
     * */
    val currentIdentifier: String

    /**
     * @return true if the current engine have created the synchronized object.
     * */
    def isCurrentEngineOwner: Boolean = currentIdentifier == ownerID

    /**
     * Send an RMI invocation based on the given agreement and invocation and waits for any result (return value or exception)
     *
     * @throws RMIException if any exception was received from the RMI response.
     * @param invocation the method's invocation information.
     * @tparam R the return type of the RMI result value.
     * @return the RMI result value
     */
    def sendInvokeAndWaitResult[R](invocation: DispatchableRemoteMethodInvocation[R]): R //TODO add a timeout. (here or in the MethodBehavior)

    /**
     * Send an RMI Invocation based on the given agreement and invocation without waiting for any result.
     *
     * @param invocation the method's invocation information.
     */
    def sendInvoke(invocation: DispatchableRemoteMethodInvocation[_]): Unit

    val network: Network //Keep an access to Network

    //TODO make this and "init" for internal use only
    def synchronizedObj(obj: AnyRef, id: Int = ThreadLocalRandom.current().nextInt()): AnyRef

    //def init(syncObject: S with SynchronizedObject[S]): Unit

    trait RMIDispatcher {
        def broadcast(args: Array[Any]): Unit

        def foreachEngines(action: String => Array[Any]): Unit
    }

}
