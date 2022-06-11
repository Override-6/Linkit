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

import fr.linkit.api.gnom.cache.sync.contract.behavior.{ConnectedObjectContext, ObjectContractFactory}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{StructureBehaviorDescriptorNode, UniqueStructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{StructureContract, SyncLevel}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException
import org.jetbrains.annotations.Nullable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](private val clazz: Class[A],
                                                       descriptors: Array[UniqueStructureContractDescriptor[A]],
                                                       val modifier: Option[ValueModifier[A]],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {
    
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
        leveledNodes(context.syncLevel).getContract(clazz, context)
        /*val mirroringInfo = if (context.syncLevel.mustBeMirrored) Some(autoDefineMirroringInfo(clazz)) else None
        val methodMap     = mutable.HashMap.empty[Int, MethodContract[Any]]
        fixUndescribedMethods(methodMap, context)
        new StructureContractImpl(clazz, mirroringInfo, methodMap.toMap, Array())*/
    }
    
    override def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A] = {
        new HeritageValueModifier(factory, limit)
    }
    
    private def ensureAllLevelsAreConcerned(): Unit = {
        descriptors.find(!_.syncLevel.isContractable) match {
            case None       =>
            case Some(desc) =>
                throw new BadContractException(s"Contract for '$clazz' have a structure descriptor contract that describes a behavior contract at a non-contractable level (${desc.syncLevel})")
        }
        /*MandatoryLevels.filter(lvl => !descriptors.exists(_.syncLevel eq lvl)).toList match {
            case Nil =>
            case lst =>
                throw new BadContractException(s"Contract for '$clazz' does not describes behavior to apply for synchronization levels: ${lst.mkString(", ")}.")
        }*/
    }
    
    private def ensureAllDescriptorsAreAtSameLevel(): Unit = {
        descriptors.foreach(d => if (d.targetClass != clazz)
            throw new BadContractException(s"Contract for $clazz contains illegal descriptor for ${d.targetClass}"))
    }
    
    private def computeLeveledNodes(): Map[SyncLevel, LeveledSBDN[A]] = {
        descriptors.map(d => {
            val level = d.syncLevel
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
    
    private class HeritageValueModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]) extends ValueModifier[A] {
        
        private val modifier = StructureBehaviorDescriptorNodeImpl.this.modifier.orNull
        
        lazy val superClassModifier: Option[ValueModifier[A]] = computeSuperClassModifier()
        lazy val interfacesModifiers                          = computeInterfacesModifiers()
        
        override def fromRemote(input: A, remote: Engine): A = {
            transform(input)(_.fromRemote(_, remote))
        }
        
        override def toRemote(input: A, remote: Engine): A = {
            transform(input)(_.toRemote(_, remote))
        }
        
        def transform(start: A)(f: (ValueModifier[A], A) => A): A = {
            var acc = start
            if (acc == null) {
                return acc
            }
            val clazz = acc.getClass
            if (clazz.isInterface) {
                for (modifier <- interfacesModifiers) {
                    handleExternalTransformation(acc, modifier)(f)
                }
            } else superClassModifier match {
                case None           => acc = f(modifier, acc)
                case Some(superMod) => handleExternalTransformation(acc, superMod)(f)
            }
            acc
        }
        
        private def handleExternalTransformation(initial: A, externalModifier: ValueModifier[A])(f: (ValueModifier[A], A) => A): A = {
            var a = f(externalModifier, initial)
            if (a == null) {
                return a
            }
            //if the modified object is not the same type as the input object
            //we get the node associated with the new object's type then continue
            //the modification algorithm with the limit set to the initial class of the input object.
            if (a.getClass != clazz)
                a = f(factory.getInstanceModifier[A](a.getClass.asInstanceOf[Class[A]], clazz), a)
            else a = f(modifier, a)
            a
        }
        
        private def computeInterfacesModifiers(): Array[ValueModifier[A]] = {
            interfaces.asInstanceOf[Array[StructureBehaviorDescriptorNodeImpl[A]]]
                    .filter(n => limit.isAssignableFrom(n.clazz))
                    .map(_.getInstanceModifier(factory, limit))
        }
        
        private def computeSuperClassModifier(): Option[ValueModifier[A]] = {
            if (!limit.isAssignableFrom(superClass.clazz)) None
            else Some(superClass.asInstanceOf[StructureBehaviorDescriptorNodeImpl[A]].getInstanceModifier(factory, limit))
        }
    }
    
    verify()
}

object StructureBehaviorDescriptorNodeImpl {
    
    val MandatoryLevels: Array[SyncLevel] = SyncLevel.values().filter(l => l != SyncLevel.Statics && l.isConnectable)
}
