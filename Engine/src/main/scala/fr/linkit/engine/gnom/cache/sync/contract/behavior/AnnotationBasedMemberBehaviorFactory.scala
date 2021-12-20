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

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation._
import fr.linkit.api.gnom.cache.sync.contract.behavior.member._
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.FieldBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.{GenericMethodBehavior, ParameterBehavior}
import fr.linkit.api.gnom.cache.sync.contract.description._
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.behavior.member.{MethodParameterBehavior, MethodReturnValueBehavior, SyncFieldBehavior}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.DefaultGenericMethodBehavior
import fr.linkit.engine.gnom.cache.sync.invokation.GenericRMIRulesAgreementBuilder

import java.lang.reflect.{Method, Parameter}
import scala.util.{Success, Try}

object AnnotationBasedMemberBehaviorFactory extends MemberBehaviorFactory {

    def getParamBehaviors(method: Method): Array[ParameterBehavior[Any]] = {
        val params = method.getParameters
                .map(genParameterBehavior)
        if (params.exists(_.isActivated))
            params
        //no behavior specified with annotation,
        //the invocation part of the system makes some optimisations for methods behaviors with empty parameter behaviors.
        else Array.empty
    }

    def genParameterBehavior(param: Parameter): ParameterBehavior[Any] = {
        val isSynchronized = param.isAnnotationPresent(classOf[Synchronized])
        new MethodParameterBehavior[Any](isSynchronized)
    }

    private def getRule(isActivated: Boolean, control: MethodControl, classDec: SyncStructureDescription[_]): BasicInvocationRule = {
        if (isActivated) control.value() else {
            classDec match {
                case desc: SyncObjectDescription[_] if desc.fullRemoteDefaultRule.nonEmpty =>
                    desc.fullRemoteDefaultRule.get
                case _                                                                     =>
                    control.value()
            }
        }
    }

    override def genMethodBehavior(procrastinator: Option[Procrastinator], desc: MethodDescription): GenericMethodBehavior = {
        val controlOpt = findMethodControl(desc)
        extractMethodControlBehavior(controlOpt.isDefined, desc, controlOpt.getOrElse(DefaultMethodControl))
    }

    private def findMethodControl(desc: MethodDescription): Option[MethodControl] = {
        var method = desc.javaMethod
        while (method ne null) {
            if (method.isAnnotationPresent(classOf[MethodControl])) {
                return Some(method.getAnnotation(classOf[MethodControl]))
            }

            def overridingMethod(clazz: Class[_]) = Try(clazz.getMethod(method.getName, method.getParameterTypes: _*))

            val declaringClass = method.getDeclaringClass

            def nextOverridingMethod(): Method = {
                Option(declaringClass.getSuperclass)
                        .map(overridingMethod) match {
                    case Some(Success(value)) => value
                    case _                    =>
                        val interfaces = declaringClass.getInterfaces
                        for (itf <- interfaces) {
                            overridingMethod(itf) match {
                                case Success(value) => return value
                                case _              =>
                            }
                        }
                        null
                }
            }

            method = nextOverridingMethod()
        }
        None
    }

    private def extractMethodControlBehavior(isActivated: Boolean, desc: MethodDescription, control: MethodControl): GenericMethodBehavior = {
        val paramBehaviors             = getParamBehaviors(desc.javaMethod)
        val rule                       = getRule(isActivated, control, desc.classDesc)
        val agreementBuilder           = rule.apply(new GenericRMIRulesAgreementBuilder())
        val isHidden                   = control.hide
        val forceLocalInnerInvocations = control.forceLocalInnerInvocations()
        val returnValueBhv             = new MethodReturnValueBehavior[Any](control.synchronizeReturnValue())

        new DefaultGenericMethodBehavior(
            isActivated, isHidden,
            forceLocalInnerInvocations, paramBehaviors, returnValueBhv, agreementBuilder
        )
    }

    override def genFieldBehavior(desc: FieldDescription): FieldBehavior[Any] = {
        val control        = Option(desc.javaField.getAnnotation(classOf[Synchronized]))
        val isSynchronized = control.isDefined
        SyncFieldBehavior(desc, isSynchronized, null)
    }

    val DefaultMethodControl: MethodControl = {
        new MethodControl {
            override def value(): BasicInvocationRule = BasicInvocationRule.ONLY_CURRENT

            override def synchronizeReturnValue(): Boolean = false

            override def hide(): Boolean = false

            override def forceLocalInnerInvocations(): Boolean = false

            override def annotationType(): Class[_ <: java.lang.annotation.Annotation] = getClass
        }
    }
}