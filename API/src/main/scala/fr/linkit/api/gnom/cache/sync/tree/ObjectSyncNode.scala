package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContract

trait ObjectSyncNode[A <: AnyRef] extends ChippedObjectNode[A] {


    /**
     * The synchronized object.
     */
    val synchronizedObject: A with SynchronizedObject[A]

}
