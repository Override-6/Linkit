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

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel._
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.contract.descriptor._
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.BadContractException
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription.isNotOverrideable
import fr.linkit.engine.internal.utils.ClassMap

import java.lang.reflect.Modifier
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ContractDescriptorDataImpl(groups: Array[ContractDescriptorGroup[AnyRef]], name: String) extends ContractDescriptorData {
    
    private val nodeMap = computeDescriptors()
    
    override val reference: ContractDescriptorReference = new ContractDescriptorReference(name)
    
    private var precompiled: Boolean = false
    
    def precompile(center: SyncClassCenter): Unit = {
        if (isPrecompiled) {
            throw new IllegalStateException("This contract descriptor data has already been pre compiled")
        }
        precompileClasses(center)
        precompiled = true
    }
    
    private def precompileClasses(center: SyncClassCenter): Unit = {
        var classes = mutable.HashSet.empty[Class[_]]
        val descs   = groups.flatMap(_.descriptors)
        
        def addClass(clazz: Class[_]): Unit = {
            if (!Modifier.isAbstract(clazz.getModifiers))
                classes += clazz
        }
        
        descs.foreach(desc => {
            val clazz = desc.targetClass
            desc match {
                case descriptor: MirroringStructureContractDescriptor[_] =>
                    classes += descriptor.mirroringInfo.stubSyncClass.mainClass
                case _                                                   =>
                    addClass(clazz)
            }
            
            desc.fields.filter(_.registrationKind == Synchronized).foreach(f => addClass(f.description.javaField.getType))
            desc.methods.foreach(method => {
                val javaMethod = method.description.javaMethod
                if (method.returnValueContract.exists(_.registrationKind == Synchronized))
                    addClass(javaMethod.getReturnType)
                val paramsContracts = method.parameterContracts
                if (paramsContracts.nonEmpty)
                    paramsContracts.zip(javaMethod.getParameterTypes)
                            .foreach { case (desc, paramType) => if (desc.registrationKind == Synchronized) addClass(paramType) }
            })
        })
        classes -= classOf[Object]
        classes = classes.filterNot(cl => center.isClassGenerated(SyncClassDef(cl)))
                .filterNot(c => isNotOverrideable(c.getModifiers))
        
        if (classes.isEmpty) {
            
            return
        }
        AppLoggers.Compilation.info(s"Found ${classes.size} classes to compile in their sync versions")
        AppLoggers.Compilation.debug("Classes to compile :")
        classes.foreach(clazz => AppLoggers.Compilation.debug(s"\tgen.${clazz}Sync"))
        center.preGenerateClasses(classes.toList.map(SyncClassDef(_)))
    }
    
    def markAsPrecompiled(): Unit = precompiled = true
    
    def isPrecompiled: Boolean = precompiled
    
    override def getNode[A <: AnyRef](clazz: Class[_]): StructureBehaviorDescriptorNode[A] = {
        nodeMap(clazz).asInstanceOf[StructureBehaviorDescriptorNode[A]]
    }
    
    private def computeDescriptors(): ClassMap[StructureBehaviorDescriptorNode[_]] = {
        val groups             = rearrangeGroups()
        val relations          = new ClassMap[ContractClassRelation[AnyRef]]()
        val objDescriptorGroup = groups.head
        if (objDescriptorGroup.clazz != classOf[Object]) {
            throw new BadContractException("No description for java.lang.Object found in given groups.")
        }
        
        val objectRelation = new ContractClassRelation[AnyRef](objDescriptorGroup.clazz, objDescriptorGroup.modifier, null)
        objDescriptorGroup.descriptors.foreach(objectRelation.addDescriptor)
        relations.put(objDescriptorGroup.clazz, objectRelation)
        for (group <- groups) if (group ne objDescriptorGroup) {
            val clazz = group.clazz
            val up    = relations.get(clazz).getOrElse(objectRelation) //should at least return the java.lang.Object behavior descriptor
            if (up.targetClass == clazz) {
                group.descriptors.foreach(up.addDescriptor)
            } else {
                val rel = new ContractClassRelation[AnyRef](clazz, group.modifier, up)
                group.descriptors.foreach(rel.addDescriptor)
                relations.put(clazz, cast(rel))
            }
        }
        for ((clazz, relation) <- relations) {
            val interfaces = clazz.getInterfaces
            for (interface <- interfaces) {
                val interfaceRelation = relations.get(interface).getOrElse(objectRelation) //should at least return the java.lang.Object behavior relation
                relation.addInterface(cast(interfaceRelation))
            }
        }
        val errorMessages = ListBuffer.empty[String]
        val result        = relations.toList.map(pair => (pair._1, try {
            pair._2.asNode
        } catch {
            case b: BadContractException =>
                errorMessages += b.getMessage
                null
        }))
        if (errorMessages.nonEmpty) {
            throw new BadContractException(s"\nBehavior Contract '$name' is invalid:\n\t- " + errorMessages.mkString("\n\t- ") + "\n")
        }
        new ClassMap[StructureBehaviorDescriptorNode[_]](result.toMap)
    }
    
    /*
    * Sorting descriptors by their hierarchy rank, and performing
    * checks to avoid multiple descriptor profiles per class
    * */
    private def rearrangeGroups(): Array[ContractDescriptorGroup[AnyRef]] = {
        type S = ContractDescriptorGroup[_]
        groups.distinct.sorted((a: S, b: S) => {
            getClassHierarchicalDepth(a.clazz) - getClassHierarchicalDepth(b.clazz)
        })
    }
    
    private def cast[X](y: Any): X = y.asInstanceOf[X]
    
    private def getClassHierarchicalDepth(clazz: Class[_]): Int = {
        if (clazz == null)
            throw new NullPointerException("clazz is null")
        if (clazz eq classOf[Object])
            return 0
        var cl    = clazz.getSuperclass
        var depth = 1
        while (cl ne null) {
            cl = cl.getSuperclass
            depth += 1
        }
        depth
    }
    
}