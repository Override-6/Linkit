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

import fr.linkit.api.gnom.cache.sync.contract.behavior.{ObjectContractFactory, RMIRulesAgreement, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{StructureBehaviorDescriptorNode, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MethodContract, StructureContract}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.contract.{BadContractException, MethodContractImpl, SimpleModifiableValueContract, StructureContractImpl}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder.EmptyBuilder
import org.jetbrains.annotations.Nullable

import java.lang.reflect.{Method, Modifier}
import scala.collection.mutable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](override val descriptor: StructureContractDescriptor[A],
                                                       @Nullable val modifier: ValueModifier[A],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    private val clazz = descriptor.targetClass
    //private val instanceDesc = SyncObjectDescription[A](clazz)
    //private val staticsDesc  = SyncStaticsDescription[A](clazz)

    override def getObjectContract(clazz: Class[_ <: A], context: SyncObjectContext): StructureContract[A] = {
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]
        val fieldMap  = mutable.HashMap.empty[Int, FieldContract[Any]]

        putMethods(methodMap, context)
        putFields(fieldMap)

        new StructureContractImpl(clazz, descriptor.mirroringInfo, methodMap.toMap, fieldMap.values.toArray)
    }

    override def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A] = {
        new HeritageValueModifier(factory, limit)
    }

    /*
    * performs verifications on the node to ensure that the resulted StructureContract would not
    * throw any exception while being used by synchronized objects.
    * */
    private def verify(): Unit = {
        ensureTypeCanBeSync(descriptor.targetClass, kind => s"illegal behavior descriptor: sync objects of type '$clazz' cannot get synchronized: ${kind} cannot be synchronized.")
        verifyMirroringHierarchy()
        ensureNoSyncFieldIsPrimitive()
        ensureNoMethodContainsPrimitiveSyncComp()
        warnAllHiddenMethodsWithDescribedBehavior()
    }

    private def warnAllHiddenMethodsWithDescribedBehavior(): Unit = {
        descriptor.methods
            .filter(_.hideMessage.isDefined)
            .filter(_.parameterContracts.nonEmpty)
            .filter(_.returnValueContract.isDefined)
            .foreach { method =>
                val javaMethod = method.description.javaMethod
                AppLogger.warn(s"Method $javaMethod is hidden but seems to contain behavior contracts in its return value nor its parameters")
            }
    }

    private def ensureNoMethodContainsPrimitiveSyncComp(): Unit = {
        descriptor.methods.foreach(method => {
            val javaMethod         = method.description.javaMethod
            val paramsContracts    = method.parameterContracts
            val paramCount         = javaMethod.getParameterCount
            val paramContractCount = paramsContracts.length
            if (!paramsContracts.isEmpty && paramCount != paramContractCount)
                throw new BadContractException(s"Contract of method $javaMethod for $clazz contains a list of the method's arguments contracts with a different length as the method's parameter count (method parameter count: $paramCount vs method's params contracts count: $paramContractCount).")
            method.parameterContracts.filter(_.isSynchronized)
                .zip(javaMethod.getParameters)
                .foreach { case (_, param) => {
                    val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' for parameter '${param.getName}' of type ${param.getType.getName}: ${kind} cannot be synchronized."
                    ensureTypeCanBeSync(param.getType, msg)
                }
                }
            if (method.returnValueContract.exists(_.isSynchronized)) {
                val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' of return type ${javaMethod.getReturnType.getName}: ${kind} cannot be synchronized."
                ensureTypeCanBeSync(javaMethod.getReturnType, msg)
            }
        })
    }

    private def ensureNoSyncFieldIsPrimitive(): Unit = {
        descriptor.fields
            .filter(_.isSynchronized)
            .foreach { field =>
                val javaField             = field.desc.javaField
                val fieldType             = javaField.getType
                val msg: String => String = kind => s"descriptor for $clazz contains an illegal sync field '${javaField.getName}' of type $fieldType: ${kind} cannot be synchronized."
                ensureTypeCanBeSync(fieldType, msg)
            }
    }

    private def ensureTypeCanBeSync(tpe: Class[_], msg: String => String): Unit = {
        if (tpe.isPrimitive) throw new BadContractException(msg("primitives"))
        if (tpe.isArray) throw new BadContractException(msg("arrays"))
        if (tpe.isEnum) throw new BadContractException(msg("enums"))
        if (tpe.isAnnotation) throw new BadContractException(msg("annotations"))
        if (tpe.isSealed) throw new BadContractException(msg("sealed classes"))

        import Modifier._
        val mods = tpe.getModifiers
        if (isFinal(mods)) throw new BadContractException(msg("final classes"))
        if (isPrivate(mods)) throw new BadContractException(msg("private classes"))
    }

    private var hierarchyVerifyResult: Option[Boolean] = None

    /**
     * @return true if super class or any interfaces are set as mirrorable
     * @throws BadContractException if this node or it's upper nodes are attributed to a descriptor that does not contains
     *                              any mirroring object information while it's upper descriptors are.
     * */
    private def verifyMirroringHierarchy(): Boolean = hierarchyVerifyResult.getOrElse {
        val superClassIsMirrorable  = if (superClass != null) {
            superClass.verifyMirroringHierarchy()
        } else false
        var interfacesAreMirrorable = false
        for (interface <- interfaces) {
            val isMirrorable = interface.verifyMirroringHierarchy()
            if (isMirrorable) interfacesAreMirrorable = true
        }
        val selfIsMirrorable = descriptor.mirroringInfo.isDefined
        if (!selfIsMirrorable && (superClassIsMirrorable || interfacesAreMirrorable)) {
            val cause = if (superClassIsMirrorable) Seq[Class[_]](superClass.clazz) else interfaces.filter(_.descriptor.mirroringInfo.isDefined).map(_.clazz).toSeq
            throw new BadContractException(s"$clazz is extending a mirrorable super class or interface (${cause.mkString("&")}) but is not set as mirrorable.\nPlease specify mirroring information for $clazz too.")
        }
        hierarchyVerifyResult = Some(selfIsMirrorable || superClassIsMirrorable || interfacesAreMirrorable)
        hierarchyVerifyResult.get
    }

    private def verifyAgreement(method: Method, agreement: RMIRulesAgreement, context: SyncObjectContext): Unit = {
        val acceptAll = agreement.isAcceptAll
        if (!acceptAll && agreement.acceptedEngines.length == 0)
            throw new BadContractException(s"method agreement $method have nowhere to invoke the method.")
        val isMirroring = descriptor.mirroringInfo.isDefined && context.ownerID != context.currentID //is mirrorable and is not origin
        if (!isMirroring) return

        if (acceptAll) {
            if (!agreement.discardedEngines.contains(context.currentID))
                throw new BadContractException(s"method agreement $method would invoke this method on this engine while its implementation is on engine ${context.ownerID}.")
        } else {
            if (agreement.acceptedEngines.contains(context.currentID))
                throw new BadContractException(s"method agreement $method would invoke this method on this engine while its implementation is on engine ${context.ownerID}.")
        }
    }

    private def putMethods(map: mutable.HashMap[Int, MethodContract[Any]], context: SyncObjectContext): Unit = {
        for (desc <- descriptor.methods) {
            val id = desc.description.methodId
            if (!map.contains(id)) {
                val agreement = desc.agreement.result(context)
                verifyAgreement(desc.description.javaMethod, agreement, context)
                val rvContract = desc.returnValueContract.getOrElse(new SimpleModifiableValueContract[Any](false))
                val contract   = new MethodContractImpl[Any](
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

    verify()
}

object StructureBehaviorDescriptorNodeImpl {

    private final val DisabledValueContract = new SimpleModifiableValueContract[Any](false, None)

    private def defaultContract(context: SyncObjectContext, desc: MethodDescription) = {
        new MethodContractImpl[Any](false, EmptyBuilder.result(context),
            Array(), DisabledValueContract, desc, None, null)
    }
}

