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

import fr.linkit.api.gnom.cache.sync.behavior.build.{BehaviorSelectionConstraint, ObjectBehaviorDescriptor}
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.MethodBehavior
import fr.linkit.engine.gnom.cache.sync.behavior.v2.constraints.AlwaysSelect

import scala.reflect.ClassTag

abstract class SyncObjectBehaviorDescriptor[T <: AnyRef](implicit tag: ClassTag[T]) extends ObjectBehaviorDescriptor[T] {

    override val targetClass: Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
    override val usingHierarchy: Array[ObjectBehaviorDescriptor[_ >: T]] = Array()
    override val withMethods   : Array[MethodBehavior] = Array()
    override val withFields    : Array[FieldBehavior[_]] = Array()
    override val constraint    : BehaviorSelectionConstraint = AlwaysSelect
}
