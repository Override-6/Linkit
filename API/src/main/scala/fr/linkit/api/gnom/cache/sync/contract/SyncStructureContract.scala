package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.behavior.SynchronizedStructureBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueMultiModifier

trait SyncStructureContract[A <: AnyRef] {

    val description: SyncStructureDescription[A]
    val behavior   : Option[SynchronizedStructureBehavior[A]]
    val modifier   : Option[ValueMultiModifier[A]]

}
