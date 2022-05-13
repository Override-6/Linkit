package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, DescriptorProfile, StructureBehaviorDescriptorNode}
import fr.linkit.engine.internal.utils.ClassMap

class ContractDescriptorDataImpl(val profiles: Array[DescriptorProfile[_]]) extends ContractDescriptorData {

    private val nodeMap = computeDescriptors()

    private var precompiled: Boolean = false

    def markAsPrecompiled(): Unit = precompiled = true

    def isPrecompiled: Boolean = precompiled

    override def getNode[A <: AnyRef](clazz: Class[_]): StructureBehaviorDescriptorNode[A] = {
        nodeMap(clazz).asInstanceOf[StructureBehaviorDescriptorNode[A]]
    }

    private def computeDescriptors(): ClassMap[StructureBehaviorDescriptorNode[_]] = {
        val descriptors      = rearrangeDescriptors()
        val relations        = new ClassMap[SyncObjectClassRelation[AnyRef]]()
        val objDescriptor = descriptors.head
        if (objDescriptor.clazz != classOf[Object])
            throw new IllegalArgumentException("Descriptions sequence's first element must be the java.lang.Object type behavior description.")

        val objectRelation = new SyncObjectClassRelation[AnyRef](objDescriptor.clazz, objDescriptor.modifier, null)
        relations.put(objDescriptor.clazz, objectRelation)
        for (profile <- descriptors.tail) {
            val clazz  = profile.clazz
            val up = relations.get(clazz).getOrElse(objectRelation) //should at least return the java.lang.Object behavior descriptor
            if (up.targetClass == clazz) {
                profile.descriptors.foreach(up.addDescriptor)
            } else {
                val rel = new SyncObjectClassRelation[AnyRef](clazz, profile.modifier, up)
                profile.descriptors.foreach(rel.addDescriptor)
                relations.put(clazz, cast(rel))
            }
        }
        for ((clazz, relation) <- relations) {
            val interfaces = clazz.getInterfaces
            for (interface <- interfaces) {
                val interfaceRelation = relations.get(interface).getOrElse(objectRelation) //should at least return the java.lang.Object behavior relation
                relation.addInterface(cast(interfaceRelation))
            }
        }
        val map = relations.map(pair => (pair._1, pair._2.asNode)).toMap
        new ClassMap[StructureBehaviorDescriptorNode[_]](map)
    }

    /*
    * Sorting descriptors by their hierarchy rank, and performing
    * checks to avoid multiple descriptor profiles per class
    * */
    private def rearrangeDescriptors(): Array[DescriptorProfile[AnyRef]] = {
        type S = DescriptorProfile[_]
        profiles.distinct.sorted((a: S, b: S) => {
            getClassHierarchicalDepth(a.clazz) - getClassHierarchicalDepth(b.clazz)
        }).asInstanceOf[Array[DescriptorProfile[AnyRef]]]
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