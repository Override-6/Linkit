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

import fr.linkit.api.gnom.cache.sync.contract.RegistrationKind._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{ConnectedObjectContext, ObjectContractFactory, RMIRulesAgreement}
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef, SyncClassDefMultiple, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{StructureBehaviorDescriptorNode, StructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MethodContract, MirroringInfo, StructureContract}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contract.{BadContractException, MethodContractImpl, SimpleModifiableValueContract, StructureContractImpl}
import fr.linkit.engine.internal.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

import java.lang.reflect.{Method, Modifier}
import scala.collection.mutable

class StructureBehaviorDescriptorNodeImpl[A <: AnyRef](override val descriptor: StructureContractDescriptor[A],
                                                       @Nullable val superClass: StructureBehaviorDescriptorNodeImpl[_ >: A],
                                                       val interfaces: Array[StructureBehaviorDescriptorNodeImpl[_ >: A]]) extends StructureBehaviorDescriptorNode[A] {

    private val clazz = descriptor.targetClass
    @Nullable val modifier: ValueModifier[A] = descriptor.modifier.orNull
    //private val instanceDesc = SyncObjectDescription[A](clazz)
    //private val staticsDesc  = SyncStaticsDescription[A](clazz)

    override def getObjectContract(clazz: Class[_ <: A], context: ConnectedObjectContext, forceMirroring: Boolean): StructureContract[A] = {
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]
        val fieldMap  = mutable.HashMap.empty[Int, FieldContract[Any]]

        putMethods(methodMap, false, context)
        putFields(fieldMap)

        val mirroringInfo = descriptor.mirroringInfo.orElse(if (forceMirroring) Some(autoDefineMirroringInfo(clazz)) else None)

        new StructureContractImpl(clazz, mirroringInfo, methodMap.toMap, fieldMap.values.toArray)
    }

    override def getStaticsContract(clazz: Class[_ <: A], context: ConnectedObjectContext): StructureContract[A] = {
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]

        putMethods(methodMap, true, context)

        new StructureContractImpl(clazz, None, methodMap.toMap, Array())
    }

    override def getInstanceModifier[L >: A](factory: ObjectContractFactory, limit: Class[L]): ValueModifier[A] = {
        new HeritageValueModifier(factory, limit)
    }

    private def autoDefineMirroringInfo(clazz: Class[_]): MirroringInfo = {
        var superClass: Class[_] = clazz
        while (findReasonTypeCantBeSync(superClass).isDefined) //will be at least java.lang.Object
            superClass = superClass.getSuperclass

        val interfaces = clazz.getInterfaces.flatMap(getSyncableInterface)
        MirroringInfo(SyncClassDefMultiple(superClass, interfaces))
    }

    private def getSyncableInterface(interface: Class[_]): Array[Class[_]] = {
        if (interface == null)
            return Array()
        if (findReasonTypeCantBeSync(interface).isDefined)
            interface.getInterfaces.flatMap(getSyncableInterface)
        else Array(interface)
    }

    private def ensureAllMethodDescribedWhenMirroring(contracts: Iterable[MethodContract[_]], clazzDef: SyncClassDef): Unit = {
        val selfIsMirrorable = descriptor.mirroringInfo.isDefined
        if (!selfIsMirrorable) return
        val classMethods = listMethods(clazzDef)
        for (contract <- contracts) {
            val contractMethod = contract.description.methodId
            if (classMethods.contains(contractMethod))
                classMethods -= contractMethod
        }
        if (classMethods.nonEmpty) {
            def str(m: Method) = m.getName + m.getParameters.map(_.getType.getName).mkString("(", ", ", ")")

            val methods = classMethods.values.map(str).mkString("- ", "\n- ", "")
            throw new BadContractException(
                s"""
                   |methods below are not bound to any contract, and the resulting connected object is a mirroring object.
                   |$methods
                   |for $clazzDef
                   |""".stripMargin)
        }
    }

    private def listMethods(syncClassDef: SyncClassDef): mutable.HashMap[Int, Method] = {
        val methods = mutable.HashMap.empty[Int, Method]

        def acc(clazz: Class[_]): Unit = {
            var cl = clazz
            while (cl != null) {
                methods ++= cl.getDeclaredMethods
                        .filter(m => !SyncObjectDescription.isNotOverrideable(m.getModifiers))
                        .map(m => (MethodDescription.computeID(m), m))
                cl = cl.getSuperclass
            }
        }

        acc(syncClassDef.mainClass)
        syncClassDef match {
            case multiple: SyncClassDefMultiple => multiple.interfaces.foreach(acc)
            case _: SyncClassDefUnique          =>
        }
        methods
    }

    /*
        * performs verifications on the node to ensure that the resulted StructureContract would not
        * throw any exception while being used by synchronized / chipped / mirroring objects.
        * */
    private def verify(): Unit = {
        //ensureTypeCanBeSync(descriptor.targetClass, kind => s"illegal behavior descriptor: sync objects of type '$clazz' cannot get synchronized: ${kind} cannot be synchronized.")
        verifyMirroringHierarchy()
        ensureNoSyncFieldIsPrimitive()
        ensureNoSyncFieldIsStatic()
        ensureNoMethodContainsPrimitiveSyncComp()
        warnAllHiddenMethodsWithDescribedBehavior()
    }

    private def ensureNoSyncFieldIsStatic(): Unit = {
        if (descriptor.fields.exists(fc => fc.registrationKind != NotRegistered && Modifier.isStatic(fc.desc.javaField.getModifiers)))
            throw new BadContractException(s"in $clazz: static synchronized fields are not supported.")
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
            method.parameterContracts.filter(_.registrationKind == Synchronized)
                    .zip(javaMethod.getParameters)
                    .foreach { case (_, param) => {
                        val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' for parameter '${param.getName}' of type ${param.getType.getName}: ${kind} cannot be synchronized."
                        ensureTypeCanBeSync(param.getType, msg)
                    }
                    }
            if (method.returnValueContract.exists(_.registrationKind == Synchronized)) {
                val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' of return type ${javaMethod.getReturnType.getName}: ${kind} cannot be synchronized."
                ensureTypeCanBeSync(javaMethod.getReturnType, msg)
            }
        })
    }

    private def ensureNoSyncFieldIsPrimitive(): Unit = {
        descriptor.fields
                .filter(_.registrationKind == Synchronized)
                .foreach { field =>
                    val javaField             = field.desc.javaField
                    val fieldType             = javaField.getType
                    val msg: String => String = kind => s"descriptor for $clazz contains an illegal sync field '${javaField.getName}' of type $fieldType: ${kind} cannot be synchronized."
                    ensureTypeCanBeSync(fieldType, msg)
                }
    }

    private def ensureTypeCanBeSync(tpe: Class[_], msg: String => String): Unit = {
        findReasonTypeCantBeSync(tpe).foreach(reason => throw new BadContractException(msg(reason)))
    }

    private def findReasonTypeCantBeSync(tpe: Class[_]): Option[String] = {
        val result = if (tpe.isPrimitive) "primitive"
        else if (tpe.isArray) "array"
        else if (tpe.isEnum) "enum"
        else if (tpe.isAnnotation) "annotation"
        else if (tpe.isSealed) "sealed class"
        else {
            import Modifier._
            val mods = tpe.getModifiers
            if (isFinal(mods)) "final class"
            else if (isPrivate(mods)) "private class"
            else if (isProtected(mods)) "protected class"
            else if (mods == 0 /*package private*/ ) "package private class"
            else null
        }
        Option(result)
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

    private def verifyAgreement(method: Method, agreement: RMIRulesAgreement, context: ConnectedObjectContext, isStatic: Boolean): Unit = {
        val acceptAll = agreement.isAcceptAll
        if (!acceptAll && agreement.acceptedEngines.length == 0)
            throw new BadContractException(s"method agreement $method have nowhere to invoke the method.")
        if (isStatic)
            return
        val isMirroring = descriptor.mirroringInfo.isDefined && context.ownerID != context.currentID //is mirrorable and is not origin = isMirroring
        if (!isMirroring) return

        val msg = s"method agreement $method would invoke this method on this engine while its implementation is on engine '${context.ownerID}'."
        if (acceptAll) {
            if (!agreement.discardedEngines.contains(context.currentID))
                throw new BadContractException(msg)
        } else {
            if (agreement.acceptedEngines.contains(context.currentID))
                throw new BadContractException(msg)
        }
    }

    private def putMethods(map: mutable.HashMap[Int, MethodContract[Any]], static: Boolean, context: ConnectedObjectContext): Unit = {
        for (desc <- descriptor.methods if Modifier.isStatic(desc.description.javaMethod.getModifiers) == static) {
            val id = desc.description.methodId
            if (!map.contains(id)) {
                val agreement = desc.agreement.result(context)
                verifyAgreement(desc.description.javaMethod, agreement, context, static)
                val rvContract = desc.returnValueContract.getOrElse(new SimpleModifiableValueContract[Any](NotRegistered))
                val contract   = new MethodContractImpl[Any](
                    desc.invocationHandlingMethod, context.choreographer, agreement, desc.parameterContracts,
                    rvContract, desc.description, desc.hideMessage, desc.procrastinator.orNull)
                map.put(id, contract)
            }
        }
        if (superClass != null)
            superClass.putMethods(map, static, context)
        interfaces.foreach(_.putMethods(map, static, context))
        if (!static)
            ensureAllMethodDescribedWhenMirroring(map.values, context.classDef)
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
