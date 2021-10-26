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

package fr.linkit.engine.gnom.cache.sync.behavior.v2

import fr.linkit.api.gnom.cache.sync.behavior.build.ObjectBehaviorDescriptor
import fr.linkit.api.gnom.cache.sync.behavior.{SynchronizedObjectBehavior, SynchronizedObjectBehaviorFactory}
import fr.linkit.engine.internal.utils.ClassMap

class SyncObjectBehaviorFactory(descriptions: Array[ObjectBehaviorDescriptor[_]]) extends SynchronizedObjectBehaviorFactory {

    private val nodeMap = createNodes(descriptions)

    override def getObjectBehavior[A <: AnyRef](clazz: Class[_]): SynchronizedObjectBehavior[A] = {
        nodeMap(clazz).getBehavior(clazz).asInstanceOf[SynchronizedObjectBehavior[A]]
    }

    private def createNodes(descriptors: Array[ObjectBehaviorDescriptor[_]]): ClassMap[BehaviorDescriptorNode[_]] = {
        descriptors
            .sortInPlace()((a, b) => getClassHierarchicalDepth(a.targetClass) - getClassHierarchicalDepth(b.targetClass))
        val relations        = new ClassMap[SyncObjectClassRelation[_]]()
        val objectDescriptor = descriptors.head
        if (objectDescriptor.targetClass != classOf[Object])
            throw new IllegalArgumentException("Descriptions must contain the java.lang.Object type behavior description.")
        relations.put(objectDescriptor.targetClass, new SyncObjectClassRelation(objectDescriptor, null))
        for (descriptor <- descriptors) {
            val clazz  = descriptor.targetClass
            val parent = relations.get(clazz).get //should at least return the java.lang.Object behavior descriptor
            relations.put(clazz, new SyncObjectClassRelation(descriptor, parent))
        }
        for ((clazz, relation) <- relations) {
            val interfaces = clazz.getInterfaces
            for (interface <- interfaces) {
                val interfaceRelation = relations.get(interface).get //should at least return the java.lang.Object behavior relation
                relation.addInterface(interfaceRelation)
            }
        }
        new ClassMap(relations.map(pair => (pair._1, pair._2.toNode)).toMap)
    }

    private def getClassHierarchicalDepth(clazz: Class[_]): Int = {
        if (clazz == null)
            throw new NullPointerException("clazz is null")
        var cl    = clazz.getSuperclass
        var depth = 0
        while (cl ne null) {
            cl = cl.getSuperclass
            depth += 1
        }
        depth
    }

}
