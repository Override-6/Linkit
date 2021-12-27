package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.StructureContractDescriptor
import fr.linkit.api.gnom.cache.sync.contract.descriptors.{StructureBehaviorDescriptorNode, ContractDescriptorData}
import fr.linkit.engine.gnom.cache.sync.contract.behavior.{StructureBehaviorDescriptorNodeImpl, SyncObjectClassRelation}
import fr.linkit.engine.internal.utils.ClassMap

class ContractDescriptorDataImpl(descriptors: Array[StructureContractDescriptor[_]]) extends ContractDescriptorData {

    private val nodeMap = createNodes(descriptors)

    override def getNode(clazz: Class[_]): StructureBehaviorDescriptorNode[_] = {
        nodeMap.get(clazz).get
    }

    private def createNodes(descriptors: Array[StructureContractDescriptor[_]]): ClassMap[StructureBehaviorDescriptorNode[_]] = {
        descriptors.sortInPlace()((a, b) => {
                    getClassHierarchicalDepth(a.targetClass) - getClassHierarchicalDepth(b.targetClass)
                })
        val relations        = new ClassMap[SyncObjectClassRelation[AnyRef]]()
        val objectDescriptor = descriptors.head
        if (objectDescriptor.targetClass != classOf[Object])
            throw new IllegalArgumentException("Descriptions must contain the java.lang.Object type behavior description.")

        val objectRelation = new SyncObjectClassRelation[AnyRef](cast(objectDescriptor), null)
        relations.put(objectDescriptor.targetClass, objectRelation)
        for (descriptor <- descriptors) {
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
