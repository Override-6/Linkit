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

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, MirroringInfo, StructureContract, SyncObjectFieldManipulation}

case class EmptyStructureContract[A <: AnyRef](override val clazz: Class[_],
                                               override val mirroringInfo: Option[MirroringInfo]) extends StructureContract[A] {

    override def findMethodContract[R](id: Int): Option[MethodContract[R]] = None

    override def applyFieldsContracts(obj: A with SynchronizedObject[A], manip: SyncObjectFieldManipulation): Unit = ()
}
