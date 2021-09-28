/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache.obj.behavior

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehaviorFactory
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.gnom.cache.sync.behavior.{ObjectBehaviorStore, ObjectBehavior}
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.application.network.Engine
import fr.linkit.engine.gnom.cache.obj.description.SimpleSyncObjectSuperClassDescription

import scala.collection.mutable
import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

class DefaultObjectBehaviorStore(override val factory: MemberBehaviorFactory) extends ObjectBehaviorStore {

    private val behaviors = mutable.HashMap.empty[Class[_], ObjectBehavior[_]]

    def this(factory: MemberBehaviorFactory, behaviors: Map[Class[_], ObjectBehavior[_]]) = {
        this(factory)
        this.behaviors ++= behaviors
    }

    override def get[B <: AnyRef : universe.TypeTag : ClassTag]: ObjectBehavior[B] = {
        getFromAnyClass(classTag[B].runtimeClass)
    }

    override def getFromClass[B <: AnyRef](clazz: Class[_]): ObjectBehavior[B] = {
        getFromAnyClass[B](clazz)
    }

    private def getFromAnyClass[B <: AnyRef](clazz: Class[_]): ObjectBehavior[B] = {
        behaviors.getOrElseUpdate(clazz, DefaultObjectBehavior[B](SimpleSyncObjectSuperClassDescription[B](clazz), this, null, null, null))
                .asInstanceOf[ObjectBehavior[B]]
    }

    private def findFromClassHereditary(clazz: Class[_]): Option[ObjectBehavior[AnyRef]] = {
        var opt = behaviors.get(clazz)
        var cl  = clazz.getSuperclass
        while (opt.isEmpty && cl != null) {
            opt = behaviors.get(cl)
            cl = cl.getSuperclass
        }
        opt.asInstanceOf[Option[ObjectBehavior[AnyRef]]]
    }

    override def findFromClass[B <: AnyRef](clazz: Class[_]): Option[ObjectBehavior[B]] = {
        behaviors.get(clazz).asInstanceOf[Option[ObjectBehavior[B]]]
    }

    override def modifyFieldForLocalComingFromLocal(enclosingObject: SynchronizedObject[_], fieldValue: AnyRef, fieldBhv: FieldBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClassHereditary(fieldBhv.desc.javaField.getType).orNull
        val fieldModifier = fieldBhv.modifier
        var obj           = fieldValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenField.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromLocal(obj, enclosingObject)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromLocal(obj, enclosingObject)
        obj
    }

    override def modifyFieldForLocalComingFromRemote(enclosingObject: SynchronizedObject[_], engine: Engine, fieldValue: AnyRef, fieldBhv: FieldBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](fieldBhv.desc.javaField.getType).orNull
        val fieldModifier = fieldBhv.modifier
        var obj           = fieldValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenField.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromRemote(obj, enclosingObject, engine)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromRemote(obj, enclosingObject, engine)
        obj
    }

    override def modifyReturnValueForLocalComingFromLocal(invocation: LocalMethodInvocation[_], returnValue: AnyRef, returnBhv: ReturnValueBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](returnBhv.tpe).orNull
        val fieldModifier = returnBhv.modifier
        var obj           = returnValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenMethodReturnValue.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromLocal(obj, invocation)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromLocal(obj, invocation)
        obj
    }

    override def modifyReturnValueForLocalComingFromRemote(invocation: LocalMethodInvocation[_], engine: Engine, returnValue: AnyRef, returnBhv: ReturnValueBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](returnBhv.tpe).orNull
        val fieldModifier = returnBhv.modifier
        var obj           = returnValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenMethodReturnValue.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromRemote(obj, invocation, engine)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromRemote(obj, invocation, engine)
        obj
    }

    override def modifyParameterForLocalComingFromLocal(args: Array[Any], paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](paramBhv.param.getType).orNull
        val fieldModifier = paramBhv.modifier
        var obj           = paramValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenParameter.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromLocal(obj, args)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromLocal(obj, args)
        obj
    }

    override def modifyParameterForLocalComingFromRemote(invocation: LocalMethodInvocation[_], engine: Engine, paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](paramBhv.param.getType).orNull
        val fieldModifier = paramBhv.modifier
        var obj           = paramValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenParameter.orNull
            if (modifier != null)
                obj = modifier.forLocalComingFromRemote(obj, invocation, engine)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forLocalComingFromRemote(obj, invocation, engine)
        obj
    }

    override def modifyParameterForRemote(invocation: LocalMethodInvocation[_], remote: Engine, paramValue: AnyRef, paramBhv: ParameterBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](paramBhv.param.getType).orNull
        val fieldModifier = paramBhv.modifier
        var obj           = paramValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenParameter.orNull
            if (modifier != null)
                obj = modifier.forRemote(obj, invocation, remote)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forRemote(obj, invocation, remote)
        obj
    }

    override def modifyReturnValueForRemote(invocation: LocalMethodInvocation[_], remote: Engine, returnValue: AnyRef, returnValueBhv: ReturnValueBehavior[AnyRef]): AnyRef = {
        val valueClassBhv = findFromClass[AnyRef](returnValueBhv.tpe).orNull
        val fieldModifier = returnValueBhv.modifier
        var obj           = returnValue
        if (valueClassBhv != null) {
            val modifier = valueClassBhv.whenMethodReturnValue.orNull
            if (modifier != null)
                obj = modifier.forRemote(obj, invocation, remote)
        }
        if (fieldModifier != null)
            obj = fieldModifier.forRemote(obj, invocation, remote)
        obj
    }
}