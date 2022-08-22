/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.contract.descriptor

import fr.linkit.api.gnom.cache.sync.contract.descriptor._
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MirroringInfo, SyncLevel}
import SyncLevel._
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException

import scala.collection.mutable.ListBuffer

class ContractClassRelation[A <: AnyRef](val targetClass: Class[A],
                                         modifier: Option[ValueModifier[A]],
                                         nextSuperRelation: ContractClassRelation[_ >: A]) {
    relation =>

    private val descriptors       = ListBuffer.empty[UniqueStructureContractDescriptor[A]]
    private val interfaceRelation = ListBuffer.empty[ContractClassRelation[_ >: A]]

    def addInterface(interface: ContractClassRelation[_ >: A]): Unit = {
        if (interface.targetClass != classOf[Object] && !interfaceRelation.contains(interface))
            interfaceRelation += interface
    }

    def addDescriptor(descriptor: StructureContractDescriptor[A]): Unit = {
        descriptor match {
            case multi: MultiStructureContractDescriptor[A]     =>
                multi.syncLevels.foreach { lvl =>
                    descriptors += uscd(lvl, descriptor)
                }
            case unique: UniqueStructureContractDescriptor[A]   => descriptors += unique
            case overall: OverallStructureContractDescriptor[A] =>
                SyncLevel.values()
                        .filter(s => s != Statics && s.isConnectable)
                        .foreach(descriptors += uscd(_, overall))
        }
    }

    private def uscd(lvl: SyncLevel, d: StructureContractDescriptor[A]): UniqueStructureContractDescriptor[A] = {
        new UniqueStructureContractDescriptor[A] {
            override val syncLevel  : SyncLevel                       = lvl
            override val autochip   : Boolean                         = d.autochip
            override val targetClass: Class[A]                        = d.targetClass
            override val methods    : Array[MethodContractDescriptor] = d.methods
            override val fields     : Array[FieldContract[Any]]       = d.fields
        }
    }

    private def mirroringFusion(a: MirroringStructureContractDescriptor[A], b: MirroringStructureContractDescriptor[A]): MirroringStructureContractDescriptor[A] = {
        type D = MirroringStructureContractDescriptor[A]
        implicit val t = (a, b)
        new MirroringStructureContractDescriptor[A] {
            override val targetClass   = t._1.targetClass
            override val mirroringInfo = fusion[D, MirroringInfo](_.mirroringInfo, _ != _, (a, _) => a, "mirroringInfo")
            override val autochip      = fusion[D, Boolean](_.autochip, _ != _, (a, _) => a, "autochip")
            override val methods       = fusion[D, Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "method contract.")
            override val fields        = fusion[D, Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "field contract.")
        }
    }

    private def mirroringFusionUnequal(a: MirroringStructureContractDescriptor[A], b: UniqueStructureContractDescriptor[A]): MirroringStructureContractDescriptor[A] = {
        type D = UniqueStructureContractDescriptor[A]
        implicit val t = (a, b)
        new MirroringStructureContractDescriptor[A] {
            override val targetClass   = t._1.targetClass
            override val mirroringInfo = a.mirroringInfo
            override val autochip      = fusion[D, Boolean](_.autochip, _ != _, (a, _) => a, "autochip")
            override val methods       = fusion[D, Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "method contract.")
            override val fields        = fusion[D, Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "field contract.")
        }
    }

    private def regularFusion(a: UniqueStructureContractDescriptor[A], b: UniqueStructureContractDescriptor[A]): UniqueStructureContractDescriptor[A] = {
        type D = UniqueStructureContractDescriptor[A]
        implicit val t = (a, b)
        new UniqueStructureContractDescriptor[A] {
            override val targetClass = t._1.targetClass
            override val autochip    = fusion[D, Boolean](_.autochip, _ != _, (a, _) => a, "autochip")
            override val syncLevel   = fusion[D, SyncLevel](_.syncLevel, _ != _, (a, _) => a, "contract at sync level")
            override val methods     = fusion[D, Array[MethodContractDescriptor]](_.methods, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "method contract")
            override val fields      = fusion[D, Array[FieldContract[Any]]](_.fields, (a, b) => a.exists(ia => b.exists(ib => ia != ib && ia.description == ib.description)), _ ++ _, "field contract")
        }
    }

    private def fusion[D <: StructureContractDescriptor[A], B]
    (extract: D => B, clashes: (B, B) => Boolean, fusion: (B, B) => B, compName: String)(implicit t: (D, D)): B = {
        val (desc, next) = t
        val a            = extract(desc)
        val b            = extract(next)
        if (clashes(a, b)) err(compName) else fusion(a, b)
    }

    def asNode: StructureBehaviorDescriptorNodeImpl[A] = {
        if (!nodeInitialized) {
            val nextSuperNode = if (nextSuperRelation == null) null else nextSuperRelation.asNode
            val descs         = descriptors.groupBy(_.syncLevel).map { case (_, descriptors) =>
                descriptors.foldLeft(null: UniqueStructureContractDescriptor[A])((a, b) => {
                    if (a == null) b else (a, b) match {
                        case (a: MirroringStructureContractDescriptor[A], b: MirroringStructureContractDescriptor[A]) =>
                            mirroringFusion(a, b)
                        case (a: MirroringStructureContractDescriptor[A], b) =>
                            mirroringFusionUnequal(a, b)
                        case (a, b: MirroringStructureContractDescriptor[A]) =>
                            mirroringFusionUnequal(b, a)
                        case _                                                  =>
                            regularFusion(a, b)
                    }
                })
            }.toArray

            val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]] = interfaceRelation.map(_.asNode).toArray

            node = try new StructureBehaviorDescriptorNodeImpl[A](targetClass, descs, modifier, nextSuperNode, interfaces)
            finally nodeInitialized = true
        }
        node
    }

    private var nodeInitialized                              = false
    private var node: StructureBehaviorDescriptorNodeImpl[A] = _

    private def err(compName: String): Nothing = {
        throw new BadContractException(s"Two Class Contracts describes different $compName. (in: ${targetClass.getName})")
    }
}