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

package fr.linkit.engine.gnom.cache.sync.contract.behavior

import fr.linkit.api.gnom.cache.sync.contract.StructureContractDescriptor
import fr.linkit.api.gnom.cache.sync.contract.behavior.SyncObjectContext
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptors.StructureBehaviorDescriptorNode
import fr.linkit.api.gnom.cache.sync.contractv2.{FieldContract, MethodContract, StructureContract}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contractv2.{MethodContractImpl, StructureContractImpl}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](override val instanceDescriptor: StructureContractDescriptor[A],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    override def foreachNodes(f: StructureBehaviorDescriptorNode[_ >: A] => Unit): Unit = {
        if (superClass != null) {
            f(superClass)
            superClass.foreachNodes(f)
        }
        interfaces.foreach(f)
    }

    private def putMethods(map: mutable.HashMap[Int, MethodContract[Any]], context: SyncObjectContext): Unit = {
        for (desc <- instanceDescriptor.methods) {
            val id = desc.description.methodId
            if (!map.contains(id)) {
                val agreement = desc.agreement.result(context)
                val contract  = new MethodContractImpl[Any](
                    desc.forceLocalInnerInvocations, agreement, desc.parameterContracts,
                    desc.returnValueContract, desc.description, desc.procrastinator, desc.isHidden)
                map.put(id, contract)
            }
        }
        if (superClass != null)
            superClass.putMethods(map, context)
        interfaces.foreach(_.putMethods(map, context))
    }

    private def putFields(map: mutable.HashMap[Int, FieldContract[Any]]): Unit = {
        for ((id, field) <- instanceDescriptor.fields) {
            if (!map.contains(id))
                map.put(id, field)
        }
        if (superClass != null)
            superClass.putFields(map)
        interfaces.foreach(_.putFields(map))
    }

    private def fillWithAnnotatedBehaviors(desc: SyncStructureDescription[A],
                                           methodMap: mutable.HashMap[Int, MethodContract[Any]],
                                           fieldMap: mutable.HashMap[Int, FieldContract[Any]],
                                           context: SyncObjectContext): Unit = {
        desc.listMethods().foreach(methodDesc => {
            val id = methodDesc.methodId
            if (!methodMap.contains(id)) {
                //TODO maybe add a default procrastinator in this node's descriptor.
                val contract = AnnotationBasedMemberBehaviorFactory.genMethodContract(None, methodDesc)(context)
                methodMap.put(id, contract)
            }
        })
        desc.listFields().foreach(field => {
            val id = field.fieldId
            if (!fieldMap.contains(id)) {
                val bhv = AnnotationBasedMemberBehaviorFactory.genFieldContract(field)
                fieldMap.put(id, bhv)
            }
        })
    }

    override def getObjectContract(clazz: Class[_], context: SyncObjectContext): StructureContract[A] = {
        val classDesc = SyncObjectDescription[A](clazz)
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]
        val fieldMap  = mutable.HashMap.empty[Int, FieldContract[Any]]

        putMethods(methodMap, context)
        putFields(fieldMap)
        fillWithAnnotatedBehaviors(classDesc, methodMap, fieldMap, context)

        new StructureContractImpl(clazz, methodMap.toMap, fieldMap.values.toArray)
    }

}