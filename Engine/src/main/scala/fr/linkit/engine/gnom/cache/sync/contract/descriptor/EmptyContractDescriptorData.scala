package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorGroup, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier

object EmptyContractDescriptorData extends ContractDescriptorDataImpl(Array(new ContractDescriptorGroup[Object] {
    override val clazz      : Class[Object]                              = classOf[Object]
    override val modifier   : Option[ValueModifier[Object]]              = None
    override val descriptors: Array[StructureContractDescriptor[Object]] = Array()
}))