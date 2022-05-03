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

package fr.linkit.api.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo}

import scala.reflect.{ClassTag, classTag}

trait StructureContractDescriptor[A <: AnyRef] {

    val targetClass  : Class[A]
    val mirroringInfo: Option[MirroringInfo]

    val methods : Array[MethodContractDescriptor]
    val fields  : Array[FieldContract[Any]]
    val modifier: Option[ValueModifier[A]]
}

object StructureContractDescriptor {

    def empty[A <: AnyRef : ClassTag]: StructureContractDescriptor[A] = {
        val clazz = classTag[A].runtimeClass.asInstanceOf[Class[A]]
        new StructureContractDescriptor[A] {
            override val targetClass  : Class[A]                        = clazz
            override val mirroringInfo: Option[MirroringInfo]           = None
            override val methods      : Array[MethodContractDescriptor] = Array()
            override val fields       : Array[FieldContract[Any]]       = Array()
            override val modifier     : Option[ValueModifier[A]]        = None
        }
    }

}