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

import fr.linkit.api.gnom.cache.sync.contract.BasicInvocationRule._
import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.behavior.{ConnectedObjectContext, RMIRulesAgreement}
import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef, SyncClassDefMultiple, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.contract.descriptor.{MirroringStructureContractDescriptor, UniqueStructureContractDescriptor}
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MethodContract, MirroringInfo, StructureContract}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod
import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObject, SynchronizedObject}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.AbstractSynchronizedObject
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.LeveledSBDN.{collectInterfaces, findReasonTypeCantBeSync, fixUndescribedMethods, getSyncableInterface}
import fr.linkit.engine.gnom.cache.sync.contract.{BadContractException, MethodContractImpl, SimpleModifiableValueContract, StructureContractImpl}
import fr.linkit.engine.gnom.cache.sync.invokation.RMIRulesAgreementGenericBuilder
import org.jetbrains.annotations.Nullable

import java.lang.reflect.{Member, Method, Modifier}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class LeveledSBDN[A <: AnyRef](@Nullable val descriptor: UniqueStructureContractDescriptor[A],
                               private val superClass: LeveledSBDN[_ >: A],
                               private val interfaces: Array[LeveledSBDN[_ >: A]]) {
    
    private lazy val clazz = descriptor.targetClass //only called if descriptor is non null
    
    def verify(): Unit = {
        if (descriptor == null) return
        verifyMirroringHierarchy()
        ensureNoSyncFieldIsPrimitive()
        ensureNoSyncFieldIsStaticOrMirroring()
        ensureNoMethodContainsPrimitiveSyncComp()
        ensureFieldsAndMethodsCorrespondToTheRightLevel()
        warnAllHiddenMethodsWithDescribedBehavior()
    }
    
    private def ensureNoSyncFieldIsStaticOrMirroring(): Unit = {
        val level = descriptor.syncLevel
        if (descriptor.fields.exists(fc => fc.registrationKind != NotRegistered && (level == Mirror || Modifier.isStatic(fc.description.javaField.getModifiers)))) {
            if (level == Mirror)
                throw new BadContractException(s"in $clazz: synchronized fields in mirroring objects are not supported.")
            throw new BadContractException(s"in $clazz: static synchronized fields are not supported.")
        }
    }
    
    private def ensureFieldsAndMethodsCorrespondToTheRightLevel(): Unit = {
        val isStatic = descriptor.syncLevel == Statics
        
        def ensureMemberIsCorrect(member: Member): Unit = {
            val isCompStatic = Modifier.isStatic(member.getModifiers)
            if (isCompStatic != isStatic) {
                throw new BadContractException(s"descriptor for $clazz contains $member that is ${if (isCompStatic) "static" else "non static"}" +
                                                       s" but the descriptor's sync level '${descriptor.syncLevel}' can only accept '${if (isStatic) "statics" else "non statics"}' methods and fields")
            }
        }
        
        descriptor.methods.map(_.description.javaMethod).foreach(ensureMemberIsCorrect)
        descriptor.fields.map(_.description.javaField).foreach(ensureMemberIsCorrect)
    }
    
    private def warnAllHiddenMethodsWithDescribedBehavior(): Unit = {
        descriptor.methods
                .filter(_.hideMessage.isDefined)
                .filter(_.parameterContracts.nonEmpty)
                .filter(_.returnValueContract.isDefined)
                .foreach { method =>
                    val javaMethod = method.description.javaMethod
                    AppLoggers.SyncObj.warn(s"Method $javaMethod is hidden but seems to contain behavior contracts in its return value nor its parameters")
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
            method.parameterContracts.filter(_.registrationKind.isConnectable)
                    .zip(javaMethod.getParameters)
                    .foreach { case (_, param) =>
                        val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' for parameter '${param.getName}' of type ${param.getType.getName}: ${kind} cannot be synchronized."
                        ensureTypeCanBeSync(param.getType, msg)
                    }
            if (method.returnValueContract.exists(_.registrationKind.isConnectable)) {
                val msg: String => String = kind => s"descriptor for $clazz contains an illegal method description '${javaMethod.getName}' with return type ${javaMethod.getReturnType.getName}: ${kind} cannot be synchronized."
                ensureTypeCanBeSync(javaMethod.getReturnType, msg)
            }
        })
    }
    
    private def ensureNoSyncFieldIsPrimitive(): Unit = {
        descriptor.fields
                .filter(_.registrationKind == Synchronized)
                .foreach { field =>
                    val javaField             = field.description.javaField
                    val fieldType             = javaField.getType
                    val msg: String => String = kind => s"descriptor for $clazz contains an illegal sync field '${javaField.getName}' of type $fieldType: ${kind} cannot be synchronized."
                    ensureTypeCanBeSync(fieldType, msg)
                }
    }
    
    private def ensureTypeCanBeSync(tpe: Class[_], msg: String => String): Unit = {
        findReasonTypeCantBeSync(tpe).foreach(reason => throw new BadContractException(msg(reason)))
    }
    
    def getContract(clazz: Class[_ <: A], context: ConnectedObjectContext): StructureContract[A] = {
        val level = descriptor.syncLevel
        if (context.syncLevel != level)
            throw new IllegalArgumentException(s"context.syncLevel != descriptor.syncLevel (${context.syncLevel} / ${level})")
        val methodMap = mutable.HashMap.empty[Int, MethodContract[Any]]
        val fieldMap  = mutable.HashMap.empty[Int, FieldContract[Any]]
        
        putMethods(methodMap, context)
        if (level != Mirror && level != Statics) //fields for Mirroring / Statics configurations not supported
            putFields(fieldMap)
        
        val mirroringInfo = descriptor match {
            case descriptor: MirroringStructureContractDescriptor[_] => Some(descriptor.mirroringInfo)
            case _                                                   =>
                if (context.syncLevel.mustBeMirrored()) Some(autoDefineMirroringInfo(clazz)) else None
        }
        //set a generic description to all methods that are not bound with any contract.
        fixUndescribedMethods(methodMap, context)
        new StructureContractImpl(clazz, mirroringInfo, methodMap.toMap, fieldMap.values.toArray)
    }
    
    private def verifyAgreement(method: Method, agreement: RMIRulesAgreement, context: ConnectedObjectContext): Unit = {
        val acceptAll = agreement.isAcceptAll
        if (!acceptAll && agreement.acceptedEngines.isEmpty)
            throw new BadContractException(s"method agreement $method have nowhere to invoke the method.")
        if (context.syncLevel == Statics)
            return
        val isMirroring = isMirrorable(descriptor) && context.ownerID != context.currentID //is mirrorable and is not origin = isMirroring
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
    
    private def putMethods(map: mutable.HashMap[Int, MethodContract[Any]], context: ConnectedObjectContext): Unit = {
        if (descriptor != null) for (desc <- descriptor.methods) {
            val id = desc.description.methodId
            if (!map.contains(id)) {
                val agreement = desc.agreementBuilder.result(context)
                verifyAgreement(desc.description.javaMethod, agreement, context)
                val rvContract = desc.returnValueContract.getOrElse(new SimpleModifiableValueContract[Any](NotRegistered))
                val contract   = new MethodContractImpl[Any](
                    desc.invocationHandlingMethod, context.choreographer, agreement, desc.parameterContracts,
                    rvContract, desc.description, desc.hideMessage, desc.procrastinator.orNull)
                map.put(id, contract)
            }
        }
        if (superClass != null)
            superClass.putMethods(map, context)
        interfaces.foreach(_.putMethods(map, context))
    }
    
    private def putFields(map: mutable.HashMap[Int, FieldContract[Any]]): Unit = {
        if (descriptor != null) for (field <- descriptor.fields) {
            val id = field.description.fieldId
            if (!map.contains(id))
                map.put(id, field)
        }
        if (superClass != null)
            superClass.putFields(map)
        interfaces.foreach(_.putFields(map))
    }
    
    private var hierarchyVerifyResult: Option[Boolean] = None
    
    private def isMirrorable(d: UniqueStructureContractDescriptor[_]) = d != null && d.isInstanceOf[MirroringStructureContractDescriptor[_]]
    
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
        val selfIsMirrorable = isMirrorable(descriptor)
        if (!selfIsMirrorable && (superClassIsMirrorable || interfacesAreMirrorable)) {
            val cause = {
                if (superClassIsMirrorable) Seq[Class[_]](superClass.clazz)
                else interfaces
                        .filter(v => isMirrorable(v.descriptor))
                        .map(_.clazz).toSeq
            }
            throw new BadContractException(s"$clazz is extending a mirrorable super class or interface (${cause.mkString("&")}) but is not set as mirrorable.\nPlease specify mirroring information for $clazz too.")
        }
        hierarchyVerifyResult = Some(selfIsMirrorable || superClassIsMirrorable || interfacesAreMirrorable)
        hierarchyVerifyResult.get
    }
    
    private def autoDefineMirroringInfo(clazz: Class[_]): MirroringInfo = {
        //add user-defined stub classes def
        val superClassStub = getFirstMirroringInfo(this)
        val interfaces     = (for (interface <- this.interfaces) yield {
            getFirstMirroringInfo(interface) match {
                case Some(stub) => (interface.clazz, stub)
                case None       => null
            }
        }).toSeq.filter(_ != null)
        var mainClass      = superClassStub match {
            case Some(info) => info.stubSyncClass.mainClass
            case None       => clazz
        }
        
        if (superClassStub.isEmpty) {
            while (findReasonTypeCantBeSync(mainClass).isDefined) //will be at least java.lang.Object
                mainClass = mainClass.getSuperclass
        }
        
        var interfaceClassStub = collectInterfaces(clazz).filterNot(cl => interfaces.exists(_._1.isAssignableFrom(cl))) flatMap (getSyncableInterface)
        if (mainClass == classOf[Object] && interfaceClassStub.nonEmpty) {
            mainClass = interfaceClassStub.head
            interfaceClassStub = interfaceClassStub.tail
        }
        MirroringInfo(SyncClassDef(mainClass, interfaceClassStub))
    }
    
    private def getFirstMirroringInfo(node: LeveledSBDN[_]): Option[MirroringInfo] = {
        var superNode = node
        while (superNode != null && (superNode.descriptor match { //take the first known mirroring info
            case md: MirroringStructureContractDescriptor[_] =>
                return Some(md.mirroringInfo)
            case _                                           =>
                true
        })) {
            superNode = superNode.superClass
        }
        None
    }
    
}

object LeveledSBDN {
    
    private final val syncClasses = Seq(classOf[ConnectedObject[_]], classOf[SynchronizedObject[_]], classOf[ChippedObject[_]], classOf[AbstractSynchronizedObject[_]])
    
    def findReasonTypeCantBeSync(tpe: Class[_]): Option[String] = {
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
            else if (!isPublic(mods)) "non public class"
            else null
        }
        Option(result)
    }
    
    private[descriptor] def collectInterfaces(clazz: Class[_]): Array[Class[_]] = {
        val buff = ListBuffer.empty[Class[_]]
        var cl   = clazz
        while (cl != null) {
            buff ++= cl.getInterfaces
            cl = cl.getSuperclass
        }
        buff.toArray
    }
    
    private[descriptor] def getSyncableInterface(interface: Class[_]): Array[Class[_]] = {
        if (interface == null)
            return Array()
        if (findReasonTypeCantBeSync(interface).isDefined)
            interface.getInterfaces.flatMap(getSyncableInterface)
        else Array(interface)
    }
    
    /**
     * Applies a default contract for methods of mirroring objects.
     * should only be applied on contexts with syncLevel set on 'Mirroring'
     * */
    def fixUndescribedMethods(methods: mutable.HashMap[Int, MethodContract[Any]], context: ConnectedObjectContext): Unit = {
        val clazzDef   = context.classDef
        val missingIds = listMethodIds(clazzDef)
        for (contract <- methods.values) {
            val contractMethodId = contract.description.methodId
            if (missingIds.contains(contractMethodId))
                missingIds -= contractMethodId
        }
        if (missingIds.isEmpty)
            return
        val desc = SyncObjectDescription(clazzDef)
        for (id <- missingIds) {
            desc.findMethodDescription(id) match {
                case Some(methodDesc) =>
                    val builder           = new RMIRulesAgreementGenericBuilder()
                    val agreement         = (if (context.syncLevel == Mirror) ONLY_ORIGIN(builder) else ONLY_CURRENT(builder)).result(context)
                    val emergencyContract = new MethodContractImpl[Any](
                        InvocationHandlingMethod.Inherit, context.choreographer,
                        agreement, Array(), SimpleModifiableValueContract.deactivated,
                        methodDesc, None, null)
                    methods.put(id, emergencyContract)
                case None             =>
            }
        }
    }
    
    private def listMethodIds(syncClassDef: SyncClassDef): mutable.HashSet[Int] = {
        val methods = mutable.HashSet.empty[Int]
        
        def acc(clazz: Class[_]): Unit = {
            if (syncClasses.contains(clazz))
                return
            clazz.getInterfaces.foreach(acc)
            var cl = clazz
            while (cl != null) {
                methods ++= cl.getDeclaredMethods
                        .filter(m => !SyncObjectDescription.isNotOverrideable(m.getModifiers))
                        .map(MethodDescription.computeID)
                cl = cl.getSuperclass
            }
        }
        
        acc(classOf[Object])
        acc(syncClassDef.mainClass)
        syncClassDef match {
            case multiple: SyncClassDefMultiple => multiple.interfaces.foreach(acc)
            case _: SyncClassDefUnique          =>
        }
        methods.filterNot(id => {
            syncClasses.exists(_.getDeclaredMethods.exists(MethodDescription.computeID(_) == id))
        })
    }
    
}
