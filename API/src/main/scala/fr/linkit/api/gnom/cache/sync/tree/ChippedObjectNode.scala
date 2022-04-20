package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.contract.StructureContract

trait ChippedObjectNode[A <: AnyRef] extends ObjectNode[A] {
    val contract : StructureContract[A]
}
