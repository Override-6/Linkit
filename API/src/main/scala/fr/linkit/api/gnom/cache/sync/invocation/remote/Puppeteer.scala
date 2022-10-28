/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.invocation.remote

import fr.linkit.api.gnom.cache.sync.{ConnectedObjectCache, ConnectedObjectReference}
import fr.linkit.api.gnom.network.{IdentifierTag, Network, NetworkFriendlyEngineTag, UniqueTag}

/**
 * The puppeteer of a SynchronizedObject creates all RMI requests and handles the results.
 * */
trait Puppeteer[S <: AnyRef] {

    /**
     * The synchronized object's node informations.
     */
    val nodeReference: ConnectedObjectReference
    /**
     * The engine's identifier that have created the synchronized object
     * */
    val ownerTag: UniqueTag with NetworkFriendlyEngineTag = nodeReference.owner

    /**
     * The object center that stores the synchronized object.
     */
    val cache: ConnectedObjectCache[_]

    /**
     * The identifier of the current engine.
     * */
    val currentEngineName: String
    val network          : Network //Keep an access to Network

    /**
     * @return true if the current engine have created the synchronized object.
     * */
    def isCurrentEngineOwner: Boolean = network.findEngine(ownerTag).exists(_.isCurrentEngine)

    /**
     * Send an RMI invocation based on the given agreement and invocation and waits for any result (return value or exception)
     *
     * @throws RMIException if any exception was received from the RMI response.
     * @param invocation the method's invocation information.
     * @tparam R the return type of the RMI result value.
     * @return the RMI result value
     */
    def sendInvokeAndWaitResult[R](invocation: DispatchableRemoteMethodInvocation[R]): R //TODO add an optional timeout.

    /**
     * Send an RMI Invocation based on the given agreement and invocation without waiting for any result.
     *
     * @param invocation the method's invocation information.
     */
    def sendInvoke(invocation: DispatchableRemoteMethodInvocation[_]): Unit

    trait RMIDispatcher {
        def broadcast(args: Array[Any]): Unit

        def foreachEngines(action: IdentifierTag => Array[Any]): Unit
    }

}
