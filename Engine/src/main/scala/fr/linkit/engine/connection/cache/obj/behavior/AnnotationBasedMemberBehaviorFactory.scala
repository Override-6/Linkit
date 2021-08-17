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

package fr.linkit.engine.connection.cache.obj.behavior

import java.lang.reflect.{Method, Parameter}

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement
import fr.linkit.api.connection.cache.obj.behavior.annotation._
import fr.linkit.api.connection.cache.obj.behavior.member._
import fr.linkit.api.connection.cache.obj.description._
import fr.linkit.api.local.concurrency.Procrastinator
import fr.linkit.engine.connection.cache.obj.invokation.SimpleRMIRulesAgreementBuilder
import fr.linkit.engine.connection.cache.obj.invokation.remote.{DefaultRMIHandler, InvokeOnlyRMIHandler}

object AnnotationBasedMemberBehaviorFactory extends MemberBehaviorFactory {

    def getSynchronizedParams(method: Method): Array[MethodParameterBehavior[Any]] = {
        val params = method.getParameters
            .map(genParameterBehavior)
        params
    }

    private def genParameterBehavior(param: Parameter): MethodParameterBehavior[Any] = {
        val isSynchronized = param.isAnnotationPresent(classOf[Synchronized])
        new MethodParameterBehavior[Any](isSynchronized, null, null)
    }

    override def genMethodBehavior(procrastinator: Option[Procrastinator], desc: MethodDescription): MethodBehavior = {
        val javaMethod         = desc.javaMethod
        val controlOpt         = Option(javaMethod.getAnnotation(classOf[MethodControl]))
        val control            = controlOpt.getOrElse(DefaultMethodControl)
        val synchronizedParams = getSynchronizedParams(desc.javaMethod)
        val rules              = Array[RemoteInvocationRule](control.value())
        val isHidden           = control.hide
        val syncReturnValue    = control.synchronizeReturnValue
        val invokeOnly         = control.invokeOnly
        val handler            = controlOpt match {
            case None    => null
            case Some(_) => if (invokeOnly) InvokeOnlyRMIHandler else DefaultRMIHandler
        }
        MethodBehavior(
            desc, synchronizedParams, syncReturnValue, isHidden,
            rules, procrastinator.orNull, handler
        )
    }

    override def genFieldBehavior(desc: FieldDescription): FieldBehavior = {
        val control        = Option(desc.javaField.getAnnotation(classOf[FieldControl]))
        val isSynchronized = control.isDefined
        FieldBehavior(desc, isSynchronized)
    }

    val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): BasicInvocationRule = BasicInvocationRule.ONLY_CURRENT

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def invokeOnly(): Boolean = false

            override def annotationType(): Class[_ <: java.lang.annotation.Annotation] = getClass
        }
    }
}