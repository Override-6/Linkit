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

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{MethodContractDescriptor, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo}
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class SyncObjectClassRelation[A <: AnyRef](val targetClass: Class[_], nextSuperRelation: SyncObjectClassRelation[_ >: A]) { relation =>

    private val descriptors       = ListBuffer.empty[StructureContractDescriptor[A]]
    private val interfaceRelation = ListBuffer.empty[SyncObjectClassRelation[_ >: A]]

    def addInterface(interface: SyncObjectClassRelation[_ >: A]): Unit = {
        if (interface.targetClass != classOf[Object] && !interfaceRelation.contains(interface))
            interfaceRelation += interface
    }

    def addDescriptor(descriptor: StructureContractDescriptor[A]): Unit = descriptors += descriptor

    lazy val asNode: StructureBehaviorDescriptorNodeImpl[A] = {
        val nextSuperNode = if (nextSuperRelation == null) null else nextSuperRelation.asNode
        val descriptor    = descriptors.foldLeft(StructureContractDescriptor.empty[A](ClassTag(targetClass)))((desc, result) => {
            def fusion[B](extract: StructureContractDescriptor[A] => B, clashes: (B, B) => Boolean, fusion: (B, B) => B, fieldName: String): B = {
                val a = extract(desc)
                val b = extract(result)
                if (clashes(a, b)) err(fieldName) else fusion(a, b)
            }

            new StructureContractDescriptor[A] {
                override val targetClass   = relation.targetClass.asInstanceOf[Class[A]]
                override val mirroringInfo = fusion[Option[MirroringInfo]](_.mirroringInfo, _.isDefined && _.isDefined, _.orElse(_), "mirroring information")
                override val modifier      = fusion[Option[ValueModifier[A]]](_.modifier, _.isDefined && _.isDefined, _.orElse(_), "value modifier.")
                override val methods       = fusion[Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(b.contains), _ ++ _, "method contract.")
                override val fields        = fusion[Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(b.contains), _ ++ _, "field contract.")
            }
        })
        new StructureBehaviorDescriptorNodeImpl[A](descriptor, nextSuperNode, interfaceRelation.map(_.asNode).toArray)
    }

    private def err(compName: String): Nothing = {
        throw new BadContractException(s"Two Structure Contract Descriptors describes different $compName.")
    }

}
