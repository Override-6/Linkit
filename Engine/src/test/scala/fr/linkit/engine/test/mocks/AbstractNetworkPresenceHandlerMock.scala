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

package fr.linkit.engine.test.mocks

import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.api.gnom.referencing.presence.{NetworkObjectPresence, NetworkPresenceHandler}
import fr.linkit.api.gnom.referencing.traffic.{LinkerRequestBundle, TrafficInterestedNPH}

class AbstractNetworkPresenceHandlerMock[R <: NetworkObjectReference](alwaysPresent: Boolean) extends NetworkPresenceHandler[R] with TrafficInterestedNPH {
    
    /**
     * tries to find a presence from
     * */
    override def getPresence(ref: R): NetworkObjectPresence = new NetworkObjectPresenceMock(alwaysPresent)
    
    override def isPresentOnEngine(engineId: String, ref: R): Boolean = alwaysPresent
    
    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        throw new UnsupportedOperationException("No response should be expected from this mocked handler.")
    }
}
