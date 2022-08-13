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

package fr.linkit.api.gnom.referencing.linker

import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}

/**
 * object linker to store network objects that could not find an attributed object linker.
 * */
trait RemainingNetworkObjectLinker extends NetworkObjectLinker[NetworkObjectReference] {

    /**
     * Saves the object in this linker
     * @param obj object to save
     * */
    def save(obj: NetworkObject[NetworkObjectReference]): Unit

    /**
     * Removes the object from this linker
     * @param reference the reference of the object to remove.
     * */
    def unsave(reference: NetworkObjectReference): Unit

}
