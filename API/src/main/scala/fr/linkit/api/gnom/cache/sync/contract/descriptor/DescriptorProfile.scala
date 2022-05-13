package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier

trait DescriptorProfile[A <: AnyRef] {
    val clazz: Class[A]
    val modifier: Option[ValueModifier[A]]
    val descriptors: Array[StructureContractDescriptor[A]]
}
