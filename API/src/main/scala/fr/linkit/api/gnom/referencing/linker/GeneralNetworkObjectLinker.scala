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

package fr.linkit.api.gnom.referencing.linker

import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH

import scala.reflect.ClassTag

trait GeneralNetworkObjectLinker extends NetworkObjectLinker[NetworkObjectReference] {

    val cacheNOL  : InitialisableNetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNPH
    val trafficNOL: NetworkObjectLinker[TrafficReference] with TrafficInterestedNPH
    val defaultNOL: DefaultNetworkObjectLinker

    def addRootLinker[R <: NetworkObjectReference: ClassTag](linker: NetworkObjectLinker[R]): Unit

    /**
     *
     * @param reference the reference to test
     * @return true if a [[NetworkObjectLinker]] (excepted remainingNOL) is able to handle the given reference.
     */
    def touchesAnyLinker(reference: NetworkObjectReference): Boolean

    def findRootLinker(reference: NetworkObjectReference): Option[NetworkObjectLinker[_ <: NetworkObjectReference]]
    
}
