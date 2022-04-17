package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.contract.behavior.IdentifierTag
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, MethodContractDescriptor, StructureBehaviorDescriptorNode, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, RemoteObjectInfo}
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.gnom.cache.sync.contract.behavior.StructureBehaviorDescriptorNodeImpl
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.MethodContractDescriptorImpl
import fr.linkit.engine.gnom.cache.sync.generation.sync.{DefaultSyncClassCenter, SyncObjectClassResource}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder

import scala.collection.mutable
import scala.reflect.ClassTag

class StaticsDedicatedContractDescriptorData(target: Engine) extends ContractDescriptorData {

    private val nodes = mutable.HashMap.empty[Class[_], StructureBehaviorDescriptorNodeImpl[_]]

    override def getNode[A <: AnyRef](clazz: Class[_]): StructureBehaviorDescriptorNode[A] = nodes.getOrElseUpdate(clazz, {
        val aClass      = clazz.asInstanceOf[Class[A]]
        val staticsDesc = new SyncStaticsDescription[A](aClass)
        val descriptor  = new StructureContractDescriptor[A] {
            override val targetClass  : Class[A]                        = aClass
            override val mirroringInfo: Option[RemoteObjectInfo]        = None
            override val methods      : Array[MethodContractDescriptor] = staticsDesc.listMethods().map(toContract).toArray
            override val fields       : Array[FieldContract[Any]]       = Array()
            override val modifier     : Option[ValueModifier[A]]        = None
        }
        new StructureBehaviorDescriptorNodeImpl[A](descriptor, null, Array())
    }).asInstanceOf[StructureBehaviorDescriptorNode[A]]

    private val agreement = new RMIRulesAgreementGenericBuilder()
        .discardAll()
        .accept(IdentifierTag(target.identifier))
        .appointReturn(IdentifierTag(target.identifier))

    private def toContract(desc: MethodDescription): MethodContractDescriptor = {
        MethodContractDescriptorImpl(desc, true, None, None, Array(), None, true, agreement)
    }
}
