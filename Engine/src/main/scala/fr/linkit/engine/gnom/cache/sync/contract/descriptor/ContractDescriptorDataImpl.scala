/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{ContractDescriptorData, ContractDescriptorGroup, ObjectContractDescriptorGroup, StructureBehaviorDescriptorNode}
import fr.linkit.engine.internal.utils.ClassMap

class ContractDescriptorDataImpl(val groups: Array[ContractDescriptorGroup[AnyRef]]) extends ContractDescriptorData {

    private val nodeMap = computeDescriptors()

    private var precompiled: Boolean = false

    def markAsPrecompiled(): Unit = precompiled = true

    def isPrecompiled: Boolean = precompiled

    override def getNode[A <: AnyRef](clazz: Class[_]): StructureBehaviorDescriptorNode[A] = {
        nodeMap(clazz).asInstanceOf[StructureBehaviorDescriptorNode[A]]
    }

    private def computeDescriptors(): ClassMap[StructureBehaviorDescriptorNode[_]] = {
        val groups      = rearrangeGroups()
        val relations        = new ClassMap[SyncObjectClassRelation[AnyRef]]()
        var objDescriptor = groups.head
        if (objDescriptor.clazz != classOf[Object]) {
            objDescriptor = ObjectContractDescriptorGroup
        }

        val objectRelation = new SyncObjectClassRelation[AnyRef](objDescriptor.clazz, objDescriptor.modifier, null)
        relations.put(objDescriptor.clazz, objectRelation)
        for (group <- groups) if (group ne objDescriptor) {
            val clazz  = group.clazz
            val up = relations.get(clazz).getOrElse(objectRelation) //should at least return the java.lang.Object behavior descriptor
            if (up.targetClass == clazz) {
                group.descriptors.foreach(up.addDescriptor)
            } else {
                val rel = new SyncObjectClassRelation[AnyRef](clazz, group.modifier, up)
                group.descriptors.foreach(rel.addDescriptor)
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
    private def rearrangeGroups(): Array[ContractDescriptorGroup[AnyRef]] = {
        type S = ContractDescriptorGroup[_]
        groups.distinct.sorted((a: S, b: S) => {
            getClassHierarchicalDepth(a.clazz) - getClassHierarchicalDepth(b.clazz)
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