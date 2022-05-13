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

package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor.{MethodContractDescriptor, MirroringStructureContractDescriptor, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo, SyncLevel}
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException

import scala.collection.mutable.ListBuffer

class SyncObjectClassRelation[A <: AnyRef](val targetClass: Class[A], modifier: Option[ValueModifier[A]], nextSuperRelation: SyncObjectClassRelation[_ >: A]) {
    relation =>

    private val descriptors       = ListBuffer.empty[StructureContractDescriptor[A]]
    private val interfaceRelation = ListBuffer.empty[SyncObjectClassRelation[_ >: A]]

    def addInterface(interface: SyncObjectClassRelation[_ >: A]): Unit = {
        if (interface.targetClass != classOf[Object] && !interfaceRelation.contains(interface))
            interfaceRelation += interface
    }

    def addDescriptor(descriptor: StructureContractDescriptor[A]): Unit = descriptors += descriptor

    def mirroringFusion(implicit t: (MirroringStructureContractDescriptor[A], MirroringStructureContractDescriptor[A])): MirroringStructureContractDescriptor[A] = {
        type D = MirroringStructureContractDescriptor[A]

        new MirroringStructureContractDescriptor[A] {
            override val targetClass   = t._1.targetClass
            override val mirroringInfo = fusion[D, MirroringInfo](_.mirroringInfo, _ != _, (a, _) => a, "mirroringInfo")
            override val methods       = fusion[D, Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(b.contains), _ ++ _, "method contract.")
            override val fields        = fusion[D, Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(b.contains), _ ++ _, "field contract.")
        }
    }

    def regularFusion(implicit t: (StructureContractDescriptor[A], StructureContractDescriptor[A])): StructureContractDescriptor[A] = {
        type D = StructureContractDescriptor[A]

        new StructureContractDescriptor[A] {
            override val targetClass = t._1.targetClass
            override val syncLevel   = fusion[D, SyncLevel](_.syncLevel, _ == _, (a, _) => a, "sync level")
            override val methods     = fusion[D, Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(b.contains), _ ++ _, "method contract.")
            override val fields      = fusion[D, Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(b.contains), _ ++ _, "field contract.")
        }
    }

    private def fusion[D <: StructureContractDescriptor[A], B]
    (extract: D => B, clashes: (B, B) => Boolean, fusion: (B, B) => B, compName: String)(implicit t: (D, D)): B = {
        val (desc, next) = t
        val a = extract(desc)
        val b = extract(next)
        if (clashes(a, b)) err(compName) else fusion(a, b)
    }


    lazy val asNode: StructureBehaviorDescriptorNodeImpl[A] = {
        val nextSuperNode = if (nextSuperRelation == null) null else nextSuperRelation.asNode
        val descs         = descriptors.groupBy(_.syncLevel).map { case (_, descriptors) =>
            descriptors.foldLeft(null: StructureContractDescriptor[A])((desc, next) => {

                (desc, next) match {
                    case (desc: MirroringStructureContractDescriptor[A], next: MirroringStructureContractDescriptor[A]) =>
                        mirroringFusion(desc, next)
                    case (null, next: MirroringStructureContractDescriptor[A])                                          =>
                        emptyDescriptor(next)
                    case (null, _)                                                                                      =>
                        emptyDescriptor(null)
                    case _                                                                                              =>
                        regularFusion(desc, next)
                }
            })
        }.toArray

        new StructureBehaviorDescriptorNodeImpl[A](targetClass, descs, modifier, nextSuperNode, interfaceRelation.map(_.asNode).toArray)
    }

    private def emptyDescriptor(next: MirroringStructureContractDescriptor[A]): StructureContractDescriptor[A] = {
        if (next == null) new StructureContractDescriptor[A] {
            override val syncLevel  : SyncLevel                       = SyncLevel.Mirroring
            override val targetClass: Class[A]                        = SyncObjectClassRelation.this.targetClass
            override val methods    : Array[MethodContractDescriptor] = Array()
            override val fields     : Array[FieldContract[Any]]       = Array()
        }
        else new MirroringStructureContractDescriptor[A] {
            override val mirroringInfo: MirroringInfo                   = next.mirroringInfo
            override val targetClass  : Class[A]                        = SyncObjectClassRelation.this.targetClass
            override val methods      : Array[MethodContractDescriptor] = Array()
            override val fields       : Array[FieldContract[Any]]       = Array()
        }
    }

    private def err(compName: String): Nothing = {
        throw new BadContractException(s"Two Structure Contract Descriptors describes different $compName.")
    }
}