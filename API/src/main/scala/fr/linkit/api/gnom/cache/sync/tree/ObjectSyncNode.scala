package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contractv2.StructureContract

trait ObjectSyncNode[A <: AnyRef] extends SyncNode[A] {

    val contract : StructureContract[A]

    /**
     * The synchronized object.
     */
    val synchronizedObject: A with SynchronizedObject[A]


}
