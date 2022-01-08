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

package fr.linkit.engine.gnom.cache.sync.contractv2

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contractv2.{FieldContract, MethodContract, StructureContract, SyncObjectFieldManipulation}

class StructureContractImpl[A <: AnyRef](override val clazz: Class[_],
                                         val methodContracts: Map[Int, MethodContract[Any]],
                                         val fieldContracts: Array[FieldContract[Any]]) extends StructureContract[A] {

    override def getMethodContract[R](id: Int): MethodContract[R] = {
        val x = methodContracts.getOrElse(id,
            throw new NoSuchElementException(s"Could not find method contract with identifier #$id for class $clazz."))
        x.asInstanceOf[MethodContract[R]]
    }

    override def applyFieldsContracts(obj: A with SynchronizedObject[A], manip: SyncObjectFieldManipulation): Unit = {
        for (contract <- fieldContracts) {
            contract.applyContract(obj.asInstanceOf[AnyRef with SynchronizedObject[AnyRef]], manip)
        }
    }
}