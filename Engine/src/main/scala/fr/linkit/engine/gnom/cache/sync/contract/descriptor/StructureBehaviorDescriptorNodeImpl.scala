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
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.LeveledSBDN.autoDefineMirroringInfo
import fr.linkit.engine.gnom.cache.sync.contract.{BadContractException, EmptyStructureContract}
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
        ensureAllDescriptorsConcernsConnectableLevel()
        ensureAllDescriptorsAreAtSameLevel()
        leveledNodes.foreach(_.verify())
    }

    override def getContract(clazz: Class[_ <: A], context: ConnectedObjectContext): StructureContract[A] = {
        leveledNodes.find(_.descriptor.syncLevel == context.syncLevel) match {
            case Some(sbdn) => sbdn.getContract(clazz, context)
            case None       =>
                val mirroringInfo = if (context.syncLevel == SyncLevel.Mirroring) Some(autoDefineMirroringInfo(clazz)) else None
                new EmptyStructureContract[A](clazz, mirroringInfo)
        }
    }

    override def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A] = {
        new HeritageValueModifier(factory, limit)
    }

    private def ensureAllDescriptorsConcernsConnectableLevel(): Unit = {
        descriptors.find(!_.syncLevel.isConnectable) match {
            case None       =>
            case Some(desc) =>
                throw new BadContractException(s"Contract for '$clazz' have a structure descriptor contract that describes a behavior contract at a non-connectable level (${desc.syncLevel})")
        }
    }

    private def ensureAllDescriptorsAreAtSameLevel(): Unit = {
        descriptors.foreach(d => if (d.targetClass != clazz)
            throw new BadContractException(s"contract for $clazz contains illegal descriptor for ${d.targetClass}"))
    }

    private def computeLeveledNodes(): Array[LeveledSBDN[A]] = {
        descriptors.map(d => {
            new LeveledSBDN(d, superClass.getMatchingSuperVerifier(d), interfaces.map(_.getMatchingSuperVerifier(d)))
        })
    }

    private def getMatchingSuperVerifier(descriptor: UniqueStructureContractDescriptor[_ <: A]): LeveledSBDN[A] = {
        leveledNodes.find(_.descriptor.syncLevel == descriptor.syncLevel).getOrElse {
            if (superClass == null) null
            else new LeveledSBDN(null,
                superClass.getMatchingSuperVerifier(descriptor),
                interfaces.map(_.getMatchingSuperVerifier(descriptor)))
        }
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
