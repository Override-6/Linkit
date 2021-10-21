/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.reference

import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence

trait DynamicNetworkObject[R <: NetworkObjectReference] extends NetworkObject[R] {

    def presence: NetworkObjectPresence
/*
    /**
     * Called when this Dynamic Network Object is about to be sent to an engine.
     * Note: The method is called by the serializer only if its attached [[NetworkObjectPresence]]
     * returned that the targeted engine does not contains any network object bound to this object's reference.
     * @param engine the targeted engine on which this object is about to be sent
     * @throws Throwable if the object can't be sent to the engine.
     * */
    @throws[Throwable]("If this object can't be send to the targeted engine.")
    def informSpreadTo(engine: Engine): Unit = ()

    /**
     * Called when this Dynamic Network Object is received from an engine.
     * Note: The method is called by the persistence system only if it's reference is not present on this engine.
     * @param engine the engine that sends the object to the current engine.
     * @throws Throwable if the object can't be accepted by the engine.
     * */
    @throws[Throwable]("If the received object can't be accepted.")
    def informSpreadFrom(engine: Engine): Unit = ()*/
}