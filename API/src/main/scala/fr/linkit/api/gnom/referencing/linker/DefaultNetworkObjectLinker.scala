package fr.linkit.api.gnom.referencing.linker

import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}

/**
 * object linker to store network objects that could not find an attributed object linker.
 * */
trait DefaultNetworkObjectLinker extends NetworkObjectLinker[NetworkObjectReference] {

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
