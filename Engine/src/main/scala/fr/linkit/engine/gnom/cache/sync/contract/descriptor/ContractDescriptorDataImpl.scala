package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, RemoteObjectInfo}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, MethodContractDescriptor, StructureBehaviorDescriptorNode, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException
import fr.linkit.engine.gnom.cache.sync.contract.behavior.{StructureBehaviorDescriptorNodeImpl, SyncObjectClassRelation}
import fr.linkit.engine.internal.utils.ClassMap

class ContractDescriptorDataImpl(descriptors: Array[StructureContractDescriptor[_]]) extends ContractDescriptorData {

    private val nodeMap = computeDescriptors()

    override def getNode[A <: AnyRef](clazz: Class[_]): StructureBehaviorDescriptorNode[A] = {
        nodeMap.get(clazz).get.asInstanceOf[StructureBehaviorDescriptorNode[A]]
    }

    private def computeDescriptors(): ClassMap[StructureBehaviorDescriptorNode[_]] = {
        val descriptors      = rearrangeDescriptors()
        val relations        = new ClassMap[SyncObjectClassRelation[AnyRef]]()
        val objectDescriptor = descriptors.head
        if (objectDescriptor.targetClass != classOf[Object])
            throw new IllegalArgumentException("Descriptions sequence's first element must be the java.lang.Object type behavior description.")

        val objectRelation = new SyncObjectClassRelation[AnyRef](cast(objectDescriptor), null)
        relations.put(objectDescriptor.targetClass, objectRelation)
        for (descriptor <- descriptors.tail) {
            val clazz  = descriptor.targetClass
            val parent = relations.get(clazz).getOrElse(objectRelation) //should at least return the java.lang.Object behavior descriptor
            relations.put(clazz, cast(new SyncObjectClassRelation(cast(descriptor), cast(parent))))
        }
        for ((clazz, relation) <- relations) {
            val interfaces = clazz.getInterfaces
            for (interface <- interfaces) {
                val interfaceRelation = relations.get(interface).getOrElse(objectRelation) //should at least return the java.lang.Object behavior relation
                relation.addInterface(cast(interfaceRelation))
            }
        }
        val map = relations.map(pair => (pair._1, pair._2.toNode)).toMap
        new ClassMap[StructureBehaviorDescriptorNode[_]](map)
    }

    /*
    * Sorting descriptors by their hierarchy rank, and performing
    * checks to avoid multiple descriptors per class
    * */
    private def rearrangeDescriptors(): Array[StructureContractDescriptor[_]] = {
        descriptors.foreach(a => {
            val clazz = a.targetClass
            val count = descriptors.count(b => clazz == b.targetClass)
            if (count > 1)
                throw new BadContractException(s"found $count descriptors for class ${a.targetClass}. Only one can be accepted")
        })
        type S = StructureContractDescriptor[_]
        descriptors.sorted((a: S, b: S) => {
            getClassHierarchicalDepth(a.targetClass) - getClassHierarchicalDepth(b.targetClass)
        })
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]

    private def getClassHierarchicalDepth(clazz: Class[_]): Int = {
        if (clazz == null)
            throw new NullPointerException("clazz is null")
        if (clazz eq classOf[Object])
            return 0
        var cl    = clazz.getSuperclass
        var depth = 1
        while (cl ne null) {
            cl = cl.getSuperclass
            depth += 1
        }
        depth
    }

}

object EmptyContractDescriptorData extends ContractDescriptorDataImpl(Array(new StructureContractDescriptor[Object] {
    override val targetClass     : Class[Object]                   = classOf[Object]
    override val remoteObjectInfo: Option[RemoteObjectInfo]        = None
    override val methods         : Array[MethodContractDescriptor] = Array()
    override val fields          : Array[FieldContract[Any]]       = Array()
    override val modifier        : Option[ValueModifier[Object]]   = None
}))