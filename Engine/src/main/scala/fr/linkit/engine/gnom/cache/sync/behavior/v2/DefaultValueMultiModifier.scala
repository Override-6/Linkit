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

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.modification.ValueMultiModifier
import fr.linkit.api.gnom.network.Engine

class DefaultValueMultiModifier[A <: AnyRef](node: BehaviorDescriptorNode[A]) extends ValueMultiModifier[A] {

    override def modifyForField(obj: A, abstractionLimit: Class[_ >: A])(containingObject: SynchronizedObject[AnyRef], causeEngine: Engine): A = {
        val clazz                                         = obj.getClass.asInstanceOf[Class[A]]
        var superNode    : BehaviorDescriptorNode[_ >: A] = node
        var modifierClass: Class[_ >: A]                  = clazz
        var result       : A                              = obj
        while (clazz != abstractionLimit) {
            val modifier = superNode.descriptor.whenField
            if (modifier.isDefined) {
                result = modifier.get.receivedFromRemote(result, containingObject, causeEngine) match {
                    case r: A => r
                }
            }
            superNode = superNode.superClass
            modifierClass = modifierClass.getSuperclass
        }
        result
    }

    override def modifyForParameter(obj: A, abstractionLimit: Class[_ >: A]): A = {
        val clazz = obj.getClass
    }

    override def modifyForMethodReturnValue(obj: A, abstractionLimit: Class[_ >: A]): A = {
        val clazz = obj.getClass
    }
}
