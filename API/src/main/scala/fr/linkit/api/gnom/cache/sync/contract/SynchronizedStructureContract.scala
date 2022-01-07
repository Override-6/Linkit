package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.behavior.SynchronizedStructureBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.contractv2.modification.ValueModifierHandler

trait SynchronizedStructureContract[A <: AnyRef] extends SynchronizedStructure[MethodContract, FieldBehavior[Any]]{

    val description: SyncStructureDescription[A]
    val behavior   : SynchronizedStructureBehavior[A]
    val modifier   : Option[ValueModifierHandler[A]]

    def getMethodContract(id: Int): Option[MethodContract]

}
