package fr.linkit.api.gnom.reference.linker

import fr.linkit.api.gnom.reference.{NetworkObject, NetworkObjectReference}

trait RemainingNetworkObjectsLinker extends NetworkObjectLinker[NetworkObjectReference] {

    def save(obj: NetworkObject[NetworkObjectReference]): Unit

    def unsave(reference: NetworkObjectReference): Unit

}
