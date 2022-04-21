package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContract

trait ChippedObjectNode[A <: AnyRef] extends ConnectedObjectNode[A] {

    val contract: StructureContract[A]
    def obj     : ChippedObject[A]
}
