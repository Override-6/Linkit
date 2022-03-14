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

import fr.linkit.api.gnom.cache.sync.contract.behavior.{ObjectContractFactory, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{StructureBehaviorDescriptorNode, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MethodContract, StructureContract}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contract.{MethodContractImpl, SimpleModifiableValueContract, StructureContractImpl}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](override val descriptor: StructureContractDescriptor[A],
                                                       @Nullable val modifier: ValueModifier[A],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    private val clazz = descriptor.targetClass

    override def foreachNodes(f: StructureBehaviorDescriptorNode[_ >: A] => Unit): Unit = {
        if (superClass != null) {
            f(superClass)
            superClass.foreachNodes(f)
        }
        interfaces.foreach(f)
    }

    private def putMethods(map: mutable.HashMap[Int, MethodContract[Any]], context: SyncObjectContext): Unit = {
        for (desc <- descriptor.methods) {
            val id = desc.description.methodId
            if (!map.contains(id)) {
                val agreement = desc.agreement.result(context)
                val rvContract = desc.returnValueContract.getOrElse(new SimpleModifiableValueContract[Any](false))
                val contract  = new MethodContractImpl[Any](
                    desc.forceLocalInnerInvocations, agreement, desc.parameterContracts,
                    rvContract, desc.description, desc.hideMessage, desc.procrastinator.orNull)
                map.put(id, contract)
            }
        }
        if (superClass != null)
            superClass.putMethods(map, context)
        interfaces.foreach(_.putMethods(map, context))
    }

    private def putFields(map: mutable.HashMap[Int, FieldContract[Any]]): Unit = {
        for (field <- descriptor.fields) {
            val id = field.desc.fieldId
            if (!map.contains(id))
                map.put(id, field)
        }
        if (superClass != null)
            superClass.putFields(map)
        interfaces.foreach(_.putFields(map))
    }

    override def getObjectContract(clazz: Class[_], context: SyncObjectContext): StructureContract[A] = {
        val classDesc = SyncObjectDescription[A](clazz)
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]
        val fieldMap  = mutable.HashMap.empty[Int, FieldContract[Any]]

        putMethods(methodMap, context)
        putFields(fieldMap)
        //fillWithAnnotatedBehaviors(classDesc, methodMap, fieldMap, context)

        new StructureContractImpl(clazz, descriptor.remoteObjectInfo, methodMap.toMap, fieldMap.values.toArray)
    }

    override def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A] = {
        new HeritageValueModifier(factory, limit)
    }

    private class HeritageValueModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]) extends ValueModifier[A] {

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
}