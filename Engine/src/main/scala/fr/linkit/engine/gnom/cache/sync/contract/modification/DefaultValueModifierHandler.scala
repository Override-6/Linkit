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

package fr.linkit.engine.gnom.cache.sync.contract.modification

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.StructureContractDescriptor
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.cache.sync.contractv2.modification.{ValueModifier, ValueModifierKind, ValueModifierHandler}
import fr.linkit.api.gnom.cache.sync.invocation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine
import fr.linkit.engine.gnom.cache.sync.contract.behavior.StructureBehaviorDescriptorNodeImpl

class DefaultValueModifierHandler[A <: AnyRef](node: StructureBehaviorDescriptorNodeImpl[A]) extends ValueModifierHandler[A] {

    override def modifyForField(obj: A, abstractionLimit: Class[_ >: A])(containingObject: SynchronizedObject[_], causeEngine: Engine): A = {
        handleModify[FieldModifier[_ >: A]](obj, abstractionLimit)(
            _.whenField,
            _.receivedFromRemote(obj, containingObject, causeEngine),
            _.receivedFromRemoteEvent(obj, containingObject, causeEngine))
    }

    override def modifyForParameter(obj: A, abstractionLimit: Class[_ >: A])(targetEngine: Engine, kind: ValueModifierKind): A = {
        handleMethodCompModify(obj, abstractionLimit)(
            _.whenParameter,
            kind
        )(targetEngine)
    }

    override def modifyForMethodReturnValue(obj: A, abstractionLimit: Class[_ >: A])(targetEngine: Engine, kind: ValueModifierKind): A = {
        handleMethodCompModify(obj, abstractionLimit)(
            _.whenMethodReturnValue,
            kind
        )(targetEngine)
    }

    private def handleMethodCompModify(@inline obj: A, @inline abstractionLimit: Class[_ >: A])
                                      (@inline getModifier: StructureContractDescriptor[_ >: A] => Option[ValueModifier[_ >: A]],
                                       @inline kind: ValueModifierKind)(
                                       @inline targetEngine: Engine): A = {
        type M = ValueModifier[_ >: A] => Any
        type E = ValueModifier[_ >: A] => Unit
        val (modify: M, event: E) = kind match {
            case ValueModifierKind.TO_REMOTE   =>
                Tuple2[M, E](_.toRemote(obj, targetEngine), _.toRemoteEvent(obj, targetEngine))
            case ValueModifierKind.FROM_REMOTE =>
                Tuple2[M, E](_.fromRemote(obj, targetEngine), _.fromRemoteEvent(obj, targetEngine))
        }
        handleModify[ValueModifier[_ >: A]](obj, abstractionLimit)(
            getModifier,
            modify,
            event)
    }

    @inline
    private def handleModify[M](@inline obj: A, @inline abstractionLimit: Class[_ >: A])
                               (@inline getModifier: StructureContractDescriptor[_ >: A] => Option[M],
                                @inline modify: M => Any,
                                @inline event: M => Unit): A = {
        val clazz                                         = obj.getClass.asInstanceOf[Class[A]]
        var superNode    : StructureBehaviorDescriptorNodeImpl[_ >: A] = node
        var modifierClass: Class[_ >: A]                               = clazz
        var result       : A                              = obj
        while (modifierClass != abstractionLimit && superNode != null) {
            val modifier = getModifier(superNode.descriptor)
            if (modifier.isDefined) {
                result = modify(modifier.get).asInstanceOf[A]
            }
            superNode = superNode.superClass
            modifierClass = modifierClass.getSuperclass
        }
        node.foreachNodes(n => getModifier(n.descriptor).fold()(event))
        superNode = node
        result
    }
}
