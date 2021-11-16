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

package fr.linkit.api.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.{InternalMethodBehavior, MethodBehavior}
import fr.linkit.api.gnom.cache.sync.behavior.modification.ValueMultiModifier
import fr.linkit.api.gnom.cache.sync.description.SyncStructureDescription

trait SynchronizedStructureBehavior[A <: AnyRef] {

    val classDesc: SyncStructureDescription[A]

    def listMethods(): Iterable[MethodBehavior]

    def getMethodBehavior(id: Int): Option[InternalMethodBehavior]

    def listField(): Iterable[FieldBehavior[AnyRef]]

    def getFieldBehavior(id: Int): Option[FieldBehavior[AnyRef]]

    val multiModifier: ValueMultiModifier[A]

}
