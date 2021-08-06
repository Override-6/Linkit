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

import fr.linkit.api.connection.cache.obj.behavior
import fr.linkit.api.connection.cache.obj.behavior.annotation._
import fr.linkit.api.connection.cache.obj.behavior.{FieldBehavior, MemberBehaviorFactory, MethodBehavior, RemoteInvocationRule}
import fr.linkit.api.connection.cache.obj.description._
import fr.linkit.engine.connection.cache.obj.invokation.local.{InvokeOnlyRMIHandler, DefaultRMIHandler}

import scala.reflect.runtime.universe

object AnnotationBasedMemberBehaviorFactory extends MemberBehaviorFactory {

    def getSynchronizedParams(symbol: universe.MethodSymbol): Seq[Boolean] = {
        val params = symbol
                .paramLists
                .flatten
                .map(_.annotations
                        .exists(_.tree
                                .tpe
                                .typeSymbol
                                .fullName == classOf[SynchronizeParam].getName)
                )
        params
    }

    override def genMethodBehavior(desc: MethodDescription): MethodBehavior = {
        val javaMethod         = desc.javaMethod
        val control            = Option(javaMethod.getAnnotation(classOf[MethodControl])).getOrElse(DefaultMethodControl)
        val synchronizedParams = getSynchronizedParams(desc.symbol)
        val invocationRules    = Array[RemoteInvocationRule](control.value())
        val isHidden           = control.hide
        val syncReturnValue    = control.synchronizeReturnValue
        val invokeOnly         = control.invokeOnly
        val handler            = if (invokeOnly) InvokeOnlyRMIHandler else DefaultRMIHandler
            behavior.MethodBehavior(
                desc, synchronizedParams, syncReturnValue, isHidden,
                invocationRules, handler
            )
    }

    override def genFieldBehavior(desc: FieldDescription): FieldBehavior = {
        val control        = Option(desc.javaField.getAnnotation(classOf[FieldControl]))
        val isSynchronized = control.exists(_.synchronize())
        behavior.FieldBehavior(desc, isSynchronized)
    }

    private val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): BasicRemoteInvocationRule = BasicRemoteInvocationRule.BLOCK_ALL

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def invokeOnly(): Boolean = false

            override def annotationType(): Class[_ <: java.lang.annotation.Annotation] = getClass
        }
    }
}