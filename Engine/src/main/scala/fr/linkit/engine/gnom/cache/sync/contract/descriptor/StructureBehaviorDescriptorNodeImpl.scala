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

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{StructureBehaviorDescriptorNode, UniqueStructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, MirrorableSyncLevel, SyncLevel}
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.StructureBehaviorDescriptorNodeImpl.MandatoryLevels
import org.jetbrains.annotations.Nullable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](private val clazz       : Class[A],
                                                       descriptors             : Array[UniqueStructureContractDescriptor[A]],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces          : Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    private lazy val leveledNodes = computeLeveledNodes()

    /*
        * performs verifications on the node to ensure that the resulted StructureContract would not
        * throw any exception while being used by synchronized / chipped / mirroring / static connected objects.
        * */
    private def verify(): Unit = {
        //ensureTypeCanBeSync(descriptor.targetClass, kind => s"illegal behavior descriptor: sync objects of type '$clazz' cannot get synchronized: ${kind} cannot be synchronized.")
        ensureAllLevelsAreConcerned()
        ensureAllDescriptorsAreAtSameLevel()
        leveledNodes.values.foreach(_.verify())
    }

    private[descriptor] def getSbdn(lvl: SyncLevel): Option[LeveledSBDN[A]] = leveledNodes.get(lvl)

    override def getContract(clazz: Class[_ <: A], context: ConnectedObjectContext): StructureContract[A] = {
        val contextLevel = context.syncLevel
        if (contextLevel == ConcreteSyncLevel.Statics && !leveledNodes.contains(contextLevel))
            throw new IllegalAccessException(s"Statics Accesses for '$clazz' not authorized.")
        leveledNodes(contextLevel).getContract(clazz, context)
    }


    private def ensureAllLevelsAreConcerned(): Unit = {
        descriptors.find(_.kind == ConcreteSyncLevel.NotRegistered) match {
            case None       =>
            case Some(desc) =>
                throw new BadContractException(s"Contract for '$clazz' have a structure descriptor contract that describes a behavior contract at a non-contractable level (${desc.kind})")
        }
        MandatoryLevels.filter(lvl => !descriptors.exists(_.kind eq lvl)) match {
            case Nil =>
            case lst =>
                throw new BadContractException(s"Contract for '$clazz' does not describes behavior to apply for synchronization levels: ${lst.mkString(", ")}.")
        }
    }

    private def ensureAllDescriptorsAreAtSameLevel(): Unit = {
        descriptors.foreach(d => if (d.targetClass != clazz)
            throw new BadContractException(s"Contract for $clazz contains illegal descriptor for ${d.targetClass}"))
    }

    private def computeLeveledNodes(): Map[SyncLevel, LeveledSBDN[A]] = {
        descriptors.map(d => {
            val level = d.kind
            val itfs  = interfaces.map(_.getLeveledNode(level))
            val sucl  = if (superClass == null) null else superClass.getLeveledNode(level)
            (level, new LeveledSBDN(d, sucl, itfs, this))
        }).toMap

    }

    /*
    private def defaultContracts(): Array[UniqueStructureContractDescriptor[A]] = {
        val classDesc = SyncObjectDescription(SyncClassDef(clazz))
        val onlyOwner = EmptyBuilder.accept(OwnerEngine).appointReturn(OwnerEngine)
        
        def rvContract(md: MethodDescription): Option[ModifiableValueContract[Any]] = {
            if (findReasonTypeCantBeSync(md.javaMethod.getReturnType).isDefined) None
            else Some(new SimpleModifiableValueContract(Mirror))
        }
        import SyncLevel._
        MandatoryLevels.filter(lvl => !descriptors.exists(_.syncLevel eq lvl)).map { lvl =>
            new UniqueStructureContractDescriptor[A] {
                
                override val syncLevel   = lvl
                override val autochip    = _
                override val targetClass = clazz
                override val fields      = Array()
                override val methods     = lvl match {
                    case Synchronized     => Array()
                    case Chipped | Mirror =>
                        classDesc.listMethods()
                                .map(md => MethodContractDescriptorImpl(md, false,
                                                                        None, rvContract(md),
                                                                        Array(), None, Inherit, onlyOwner)).toArray
                }
            }
        }
    }*/

    private def getLeveledNode(level: SyncLevel): LeveledSBDN[A] = {
        leveledNodes.getOrElse(level, {
            if (superClass == null) null
            else new LeveledSBDN(null,
                superClass.getLeveledNode(level),
                interfaces.map(_.getLeveledNode(level)), this)
        })
    }

    verify()
}

object StructureBehaviorDescriptorNodeImpl {

    import ConcreteSyncLevel._
    import MirrorableSyncLevel._
    val MandatoryLevels: List[SyncLevel] = Mirror :: Chipped :: Synchronized :: Nil
}
