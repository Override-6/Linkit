package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.ChippedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer

trait ChippedObjectNode[A <: AnyRef] extends ConnectedObjectNode[A] {

    val choreographer: InvocationChoreographer
    val contract: StructureContract[A]
    def obj     : ChippedObject[A]
}
