package fr.linkit.engine.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.{ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.cache.sync.env.SyncObjectCompanion
import fr.linkit.api.gnom.referencing.{NamedIdentifier, NetworkObject}


class SecondRegistryLayer(registry: ConnectedObjectRegistryImpl[_]) extends ConnectedObjectRegistryLayer[AnyRef](registry) {
}
