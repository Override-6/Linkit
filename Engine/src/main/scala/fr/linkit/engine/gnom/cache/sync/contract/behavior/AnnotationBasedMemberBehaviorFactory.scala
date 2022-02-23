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
import fr.linkit.api.gnom.cache.sync.contract.behavior.{MemberContractFactory, SyncObjectContext}
import fr.linkit.api.gnom.cache.sync.contract.description._
import fr.linkit.api.gnom.cache.sync.contract.modification.ValueModifier
import fr.linkit.api.gnom.cache.sync.contract.{FieldContract, MethodContract, ValueContract}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contractv2.{FieldContractImpl, MethodContractImpl, SimpleValueContract}
import fr.linkit.engine.gnom.cache.sync.invokation.GenericRMIRulesAgreementBuilder

import java.lang.reflect.{Method, Parameter}
import scala.util.{Success, Try}

object AnnotationBasedMemberBehaviorFactory extends MemberContractFactory {

    def genParamContracts(method: Method): Array[ValueContract[Any]] = {
        val params = method.getParameters
                .map(genParameterContract)
        if (params.exists(_.isSynchronized))
            params
        //no behavior specified with annotation,
        //the invocation part of the system makes some optimisations for methods behaviors with empty parameter behaviors.
        else Array.empty
    }

    def genParameterContract(param: Parameter): ValueContract[Any] = {
        new ValueContract[Any] {
            override val isSynchronized: Boolean                    = param.isAnnotationPresent(classOf[Synchronized])
            override val modifier      : Option[ValueModifier[Any]] = None
        }
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

    override def genMethodContract(procrastinator: Option[Procrastinator], desc: MethodDescription): SyncObjectContext => MethodContract[Any] = {
        val controlOpt = findMethodControl(desc)
        extractMethodControlContract(controlOpt.isDefined, desc, controlOpt.getOrElse(DefaultMethodControl))
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

    private def extractMethodControlContract(isActivated: Boolean, desc: MethodDescription, control: MethodControl): SyncObjectContext => MethodContract[Any] = {
        val paramContracts             = genParamContracts(desc.javaMethod)
        val rule                       = getRule(isActivated, control, desc.classDesc)
        val agreementBuilder           = rule.apply(new GenericRMIRulesAgreementBuilder())
        val isHidden                   = control.hide
        val forceLocalInnerInvocations = control.forceLocalInnerInvocations()
        val returnValueContract        = new SimpleValueContract[Any](control.synchronizeReturnValue())

        context => {
            val agreement = agreementBuilder.result(context)
            new MethodContractImpl[Any](
                forceLocalInnerInvocations, agreement, paramContracts,
                returnValueContract, desc, None, null, isHidden)
        }
    }

    override def genFieldContract(desc: FieldDescription): FieldContract[Any] = {
        val control        = Option(desc.javaField.getAnnotation(classOf[Synchronized]))
        val isSynchronized = control.isDefined
        new FieldContractImpl[Any](desc, None, isSynchronized)
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