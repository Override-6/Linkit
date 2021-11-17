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

package fr.linkit.api.gnom.cache.sync.contract.behavior.build

import fr.linkit.api.gnom.cache.sync.contract.behavior.ValueBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.InternalMethodBehavior

trait ObjectBehaviorDescriptor[T <: AnyRef] extends ValueBehavior[T] {

    val targetClass: Class[T]

    val usingHierarchy: Array[ObjectBehaviorDescriptor[_ >: T]]
    val withMethods   : Array[InternalMethodBehavior]
    val withFields    : Array[FieldBehavior[AnyRef]]
}
