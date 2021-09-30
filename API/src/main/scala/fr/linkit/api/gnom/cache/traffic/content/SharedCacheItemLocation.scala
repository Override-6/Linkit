package fr.linkit.api.gnom.cache.traffic.content

import fr.linkit.api.gnom.reference.NetworkReferenceLocation

trait SharedCacheItemLocation extends NetworkReferenceLocation {

    val cacheFamily: String
    val cacheID    : Int

}
