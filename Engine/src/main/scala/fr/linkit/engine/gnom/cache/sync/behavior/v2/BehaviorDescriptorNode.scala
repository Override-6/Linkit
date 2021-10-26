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

import fr.linkit.api.gnom.cache.sync.behavior.SynchronizedObjectBehavior
import fr.linkit.api.gnom.cache.sync.behavior.build.ObjectBehaviorDescriptor
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.gnom.cache.sync.behavior.modification.ValueMultiModifier
import fr.linkit.api.gnom.cache.sync.description.SyncObjectSuperclassDescription
import fr.linkit.api.gnom.cache.sync.tree.SyncNode
import fr.linkit.engine.gnom.cache.sync.behavior.{AbstractSynchronizedObjectBehavior, AnnotationBasedMemberBehaviorFactory}
import fr.linkit.engine.gnom.cache.sync.description.SimpleSyncObjectSuperClassDescription
import org.jetbrains.annotations.NotNull

import scala.collection.mutable

class BehaviorDescriptorNode[A <: AnyRef](val descriptor: ObjectBehaviorDescriptor[A],
                                          val superClass: BehaviorDescriptorNode[_ >: A],
                                          val interfaces: Array[BehaviorDescriptorNode[_ >: A]]) {

    private def putMethods(map: mutable.HashMap[Int, InternalMethodBehavior]): Unit = {
        for (method <- descriptor.withMethods) {
            val id = method.desc.methodId
            if (!map.contains(id))
                map.put(id, method)
        }
        superClass.putMethods(map)
        interfaces.foreach(_.putMethods(map))
    }

    private def putFields(map: mutable.HashMap[Int, FieldBehavior[AnyRef]]): Unit = {
        for (field <- descriptor.withFields) {
            val id = field.desc.fieldId
            if (!map.contains(id))
                map.put(id, field)
        }
        superClass.putFields(map)
        interfaces.foreach(_.putFields(map))
    }

    private def fillWithDefaultBehaviors(desc: SyncObjectSuperclassDescription[A],
                                         methodMap: mutable.HashMap[Int, InternalMethodBehavior],
                                         fieldMap: mutable.HashMap[Int, FieldBehavior[AnyRef]]): Unit = {
        desc.listMethods().foreach(method => {
            val id = method.methodId
            if (!methodMap.contains(id)) { //TODO maybe add a default procrastinator in this node's descriptor.
                val bhv = AnnotationBasedMemberBehaviorFactory.genMethodBehavior(null, method)
                methodMap.put(id, bhv)
            }
        })
        desc.listFields().foreach(field => {
            val id = field.fieldId
            if (!fieldMap.contains(id)) {
                val bhv = AnnotationBasedMemberBehaviorFactory.genFieldBehavior(field)
                fieldMap.put(id, bhv)
            }
        })
    }

    def getBehavior(@NotNull theObject: AnyRef, nodeParent: SyncNode[_]): SynchronizedObjectBehavior[A] = {
        val classDesc = SimpleSyncObjectSuperClassDescription[A](theObject.getClass)
        val methodMap = mutable.HashMap.empty[Int, InternalMethodBehavior]
        val fieldMap  = mutable.HashMap.empty[Int, FieldBehavior[AnyRef]]

        putMethods(methodMap)
        putFields(fieldMap)
        fillWithDefaultBehaviors(classDesc, methodMap, fieldMap)

        new AbstractSynchronizedObjectBehavior[A](classDesc) {
            override protected val methods: Map[Int, InternalMethodBehavior] = methodMap.toMap
            override protected val fields : Map[Int, FieldBehavior[AnyRef]]  = fieldMap.toMap

            override val multiModifier: ValueMultiModifier[A] = new DefaultValueMultiModifier[A](BehaviorDescriptorNode.this)
        }
    }
}