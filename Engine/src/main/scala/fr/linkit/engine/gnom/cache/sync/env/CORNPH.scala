package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler

trait CORNPH extends NetworkPresenceHandler[ConnectedObjectReference] {
    def registerReference(ref: ConnectedObjectReference): Unit


    def unregisterReference(ref: ConnectedObjectReference): Unit
}
