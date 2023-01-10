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

package fr.linkit.api.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence

/**
 * The node of a synchronized object.
 *
 * @tparam A the super type of the synchronized object
 */
trait ConnectedObjectCompanion[A <: AnyRef] {

    val presence: NetworkObjectPresence

    val reference: ConnectedObjectReference

    /**
     * This node's identifier
     */
    val id: NamedIdentifier

    /**
     * The engine identifier that owns the synchronized object.
     * (The owner is usually the engine that have created the object)
     */
    val ownerTag: UniqueTag with NetworkFriendlyEngineTag

    def obj: ConnectedObject[A]

}
