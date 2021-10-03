/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.behavior.RemoteInvocationRule
import fr.linkit.api.gnom.cache.sync.behavior.annotation._
import fr.linkit.api.gnom.cache.sync.behavior.member._
import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.description._
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.behavior.member.{MethodParameterBehavior, SyncFieldBehavior, SyncMethodBehavior}
import fr.linkit.engine.gnom.cache.sync.invokation.DefaultMethodInvocationHandler

import java.lang.reflect.{Method, Parameter}

object AnnotationBasedMemberBehaviorFactory extends MemberBehaviorFactory {

    def getSynchronizedParams(method: Method): Array[ParameterBehavior[AnyRef]] = {
        val params = method.getParameters
                .map(genParameterBehavior)
        params
    }

    private def genParameterBehavior(param: Parameter): ParameterBehavior[AnyRef] = {
        val isSynchronized = param.isAnnotationPresent(classOf[Synchronized])
        new MethodParameterBehavior[AnyRef](param, isSynchronized, null)
    }

    override def genMethodBehavior(procrastinator: Option[Procrastinator], desc: MethodDescription): SyncMethodBehavior = {
        val javaMethod         = desc.method
        val controlOpt         = Option(javaMethod.getAnnotation(classOf[MethodControl]))
        val control            = controlOpt.getOrElse(DefaultMethodControl)
        val synchronizedParams = getSynchronizedParams(desc.method)
        val rules              = Array[RemoteInvocationRule](control.value())
        val isHidden           = control.hide
        val innerInvocations   = control.innerInvocations()
        val syncReturnValue    = control.synchronizeReturnValue
        val handler            = controlOpt match {
            case None    => null
            case Some(_) => DefaultMethodInvocationHandler
        }
        SyncMethodBehavior(
            desc, synchronizedParams, syncReturnValue, isHidden,
            innerInvocations, rules, procrastinator.orNull, handler
        )
    }

    override def genFieldBehavior(desc: FieldDescription): FieldBehavior[AnyRef] = {
        val control        = Option(desc.javaField.getAnnotation(classOf[Synchronized]))
        val isSynchronized = control.isDefined
        SyncFieldBehavior(desc, isSynchronized, null)
    }

    val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): BasicInvocationRule = BasicInvocationRule.ONLY_CURRENT

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def innerInvocations(): Boolean = false

            override def annotationType(): Class[_ <: java.lang.annotation.Annotation] = getClass
        }
    }
}