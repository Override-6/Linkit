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

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.MethodControl
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.{GenericMethodBehavior, MethodBehavior, UsageMethodBehavior}
import fr.linkit.api.gnom.cache.sync.contract.behavior.{SyncObjectContext, SynchronizedStructureBehavior}
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.contract.descriptors.StructureBehaviorDescriptorNode
import fr.linkit.api.gnom.cache.sync.contract.modification.{MethodCompModifier, ValueMultiModifier}
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, ParameterContract, StructureContractDescriptor, SynchronizedStructureContract}
import fr.linkit.api.gnom.cache.sync.invokation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.behavior.AnnotationBasedMemberBehaviorFactory.DefaultMethodControl
import fr.linkit.engine.gnom.cache.sync.contract.behavior.member.MethodParameterBehavior
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contract.modification.DefaultValueMultiModifier
import fr.linkit.engine.gnom.cache.sync.contract.{AbstractSynchronizedStructure, MethodParameterContract}
import fr.linkit.engine.gnom.cache.sync.invokation.{DefaultMethodInvocationHandler, GenericRMIRulesAgreementBuilder}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](override val descriptor: StructureContractDescriptor[A],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    override def foreachNodes(f: StructureBehaviorDescriptorNode[_ >: A] => Unit): Unit = {
        if (superClass != null) {
            f(superClass)
            superClass.foreachNodes(f)
        }
        interfaces.foreach(f)
    }

    private def putMethods(map: mutable.HashMap[Int, MethodContract], context: SyncObjectContext): Unit = {
        for (contractDesc <- descriptor.withMethods) {
            val id = contractDesc.description.methodId
            if (!map.contains(id)) {
                val behavior = contractDesc.behavior match {
                    case behavior: GenericMethodBehavior => behavior.toUsage(context)
                    case behavior: UsageMethodBehavior   => behavior
                }
                map.put(id, new MethodContract {
                    override val behavior           : UsageMethodBehavior           = behavior
                    override val description        : MethodDescription             = contractDesc.description
                    override val parameterContracts : Array[ParameterContract[Any]] = contractDesc.parameterContracts
                    override val returnValueModifier: MethodCompModifier[Any]       = contractDesc.returnValueModifier
                    override val procrastinator     : Procrastinator                = contractDesc.procrastinator
                    override val handler            : MethodInvocationHandler       = contractDesc.handler
                })
            }
        }
        if (superClass != null)
            superClass.putMethods(map, context)
        interfaces.foreach(_.putMethods(map, context))
    }

    private def putFields(map: mutable.HashMap[Int, FieldBehavior[Any]]): Unit = {
        for (field <- descriptor.withFields) {
            val id = field.desc.fieldId
            if (!map.contains(id))
                map.put(id, field)
        }
        if (superClass != null)
            superClass.putFields(map)
        interfaces.foreach(_.putFields(map))
    }

    private def getParameterContracts(methodDesc: MethodDescription, methodBehavior: MethodBehavior): Array[ParameterContract[Any]] = {
        val params         = methodDesc.params
        val paramBehaviors = methodBehavior.parameterBehaviors
        val paramBhvCount  = paramBehaviors.length
        (for (i <- methodDesc.params.indices) yield {
            val param         = params(i)
            val paramBehavior = if (i < paramBhvCount) paramBehaviors(i) else MethodParameterBehavior[Any](false)

            MethodParameterContract[Any](param, Some(paramBehavior), None)
        }).toArray
    }

    private def fillWithAnnotatedBehaviors(desc: SyncStructureDescription[A],
                                           methodMap: mutable.HashMap[Int, MethodContract],
                                           fieldMap: mutable.HashMap[Int, FieldBehavior[Any]],
                                           context: SyncObjectContext): Unit = {
        desc.listMethods().foreach(methodDesc => {
            val id = methodDesc.methodId
            if (!methodMap.contains(id)) {
                //TODO maybe add a default procrastinator in this node's descriptor.
                val bhv                 = AnnotationBasedMemberBehaviorFactory.genMethodBehavior(None, methodDesc).toUsage(context)
                val parameterContracts0 = getParameterContracts(methodDesc, bhv)
                methodMap.put(id, new MethodContract {
                    override val description        : MethodDescription             = methodDesc
                    override val behavior           : UsageMethodBehavior           = bhv
                    override val parameterContracts : Array[ParameterContract[Any]] = parameterContracts0
                    override val returnValueModifier: MethodCompModifier[Any]       = null
                    override val procrastinator     : Procrastinator                = null
                    override val handler            : MethodInvocationHandler       = DefaultMethodInvocationHandler
                })
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

    override def getContract(clazz: Class[_], context: SyncObjectContext): SynchronizedStructureContract[A] = {
        val classDesc = SyncObjectDescription[A](clazz)
        val methodMap = mutable.HashMap.empty[Int, MethodContract]
        val fieldMap  = mutable.HashMap.empty[Int, FieldBehavior[Any]]

        putMethods(methodMap, context)
        putFields(fieldMap)
        fillWithAnnotatedBehaviors(classDesc, methodMap, fieldMap, context)

        val bhv = new AbstractSynchronizedStructure[UsageMethodBehavior, FieldBehavior[Any]] with SynchronizedStructureBehavior[A] {
            override protected val methods: Map[Int, UsageMethodBehavior] = methodMap.view.mapValues(_.behavior).toMap
            override protected val fields : Map[Int, FieldBehavior[Any]]  = fieldMap.toMap

            override def getFieldBehavior(id: Int): Option[FieldBehavior[Any]] = getField(id)

            override def getMethodBehavior(id: Int): Option[UsageMethodBehavior] = getMethod(id)
        }

        new AbstractSynchronizedStructure[MethodContract, FieldBehavior[Any]]() with SynchronizedStructureContract[A] {
            override protected val methods: Map[Int, MethodContract]     = methodMap.toMap
            override protected val fields : Map[Int, FieldBehavior[Any]] = fieldMap.toMap

            override val description: SyncStructureDescription[A]      = SyncObjectDescription(clazz)
            override val behavior   : SynchronizedStructureBehavior[A] = bhv
            override val modifier   : Option[ValueMultiModifier[A]]    = Some(new DefaultValueMultiModifier[A](StructureBehaviorDescriptorNodeImpl.this))

            override def getMethodContract(id: Int): Option[MethodContract] = getMethod(id)
        }
    }
}