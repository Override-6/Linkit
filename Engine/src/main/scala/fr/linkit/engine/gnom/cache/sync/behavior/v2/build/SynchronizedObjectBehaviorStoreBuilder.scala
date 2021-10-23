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

package fr.linkit.engine.gnom.cache.sync.behavior.v2.build

import fr.linkit.api.gnom.cache.sync.behavior.RemoteInvocationRule
import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule.BROADCAST
import fr.linkit.api.gnom.cache.sync.behavior.build.ObjectBehaviorDescriptor
import fr.linkit.api.gnom.cache.sync.behavior.member.method.MethodBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import fr.linkit.api.gnom.cache.sync.description.{MethodDescription, SyncObjectSuperclassDescription}
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.behavior.AnnotationBasedMemberBehaviorFactory
import fr.linkit.engine.gnom.cache.sync.behavior.member.{MethodParameterBehavior, SyncMethodBehavior}
import fr.linkit.engine.gnom.cache.sync.behavior.v2.build.SynchronizedObjectBehaviorStoreBuilder.{MethodBehaviorBuilder, Recognizable}
import fr.linkit.engine.gnom.cache.sync.invokation.DefaultMethodInvocationHandler

import java.lang.reflect.{Method, Parameter}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class SynchronizedObjectBehaviorStoreBuilder {

    private val descriptors    = mutable.HashMap.empty[Any, ObjectBehaviorDescriptor[_]]
    private var defaultID: Int = 0

    def describe(builder: SyncObjectBehaviorDescriptorBuilder): Unit = {
        val tag = builder.tag.getOrElse(nextDescriptorDefaultID)
        descriptors.put(tag, builder)
    }

    private def nextDescriptorDefaultID: Int = {
        defaultID += 1
        defaultID
    }

    abstract class SyncObjectBehaviorDescriptorBuilder[T <: AnyRef](override val tag: Option[Any])(desc: SyncObjectSuperclassDescription[T]) extends Recognizable {

        private val clazz           = desc.clazz
        private val methodBehaviors = mutable.HashMap.empty[Method, SyncMethodBehavior]

        def enable: MemberBehaviorBuilder = {
            new MemberBehaviorBuilder()
        }

        class MemberBehaviorBuilder {

            class MethodBehaviorDescriptorBuilderIntroduction(desc: MethodDescription) {

                private var concluded = false

                def as(methodName: String): Unit = conclude {
                    val bhv = methodBehaviors.getOrElse(getMethod(methodName).javaMethod, {
                        throw new NoSuchElementException(s"Method '$methodName' not described. its behavior must be described before.")
                    })
                    SyncMethodBehavior.copy(desc, bhv)
                }

                def as(builder: MethodBehaviorBuilder): Unit = conclude {
                    builder.setContext(desc)
                    builder.build()
                }

                def withRule(rule: RemoteInvocationRule): Unit = conclude {
                    SyncMethodBehavior(
                        desc, Array.empty,
                        null, false, false,
                        Array(rule), null, DefaultMethodInvocationHandler)
                }

                private def conclude(conclusion: (=> SyncMethodBehavior)): Unit = {
                    if (concluded)
                        throw new IllegalStateException("This method was already described.")
                    val methodBehavior = conclusion
                    methodBehaviors.put(methodBehavior.desc.javaMethod, methodBehavior)
                    concluded = true
                }

            }

            private def getMethod(name: String): MethodDescription = {
                desc.findMethodDescription(name).getOrElse {
                    throw new NoSuchElementException(s"Can not find declared or inherited method '$name' in $clazz")
                }
            }

            def method(name: String): MethodBehaviorDescriptorBuilderIntroduction = {
                val method = getMethod(name)
                new MethodBehaviorDescriptorBuilderIntroduction(method)
            }
        }

        enable method "test" withRule BROADCAST
    }

}

object SynchronizedObjectBehaviorStoreBuilder {

    sealed trait Recognizable {

        val tag: Option[Any]
    }

    abstract class MethodBehaviorBuilder extends AbstractBehaviorBuilder[MethodDescription] {

        private val paramBehaviors             = mutable.HashMap.empty[Parameter, ParameterBehavior[AnyRef]]
        private var usedParams: Option[params] = None

        private var innerInvocations: Boolean                     = false
        private var rules           : Array[RemoteInvocationRule] = Array.empty
        private var procrastinator  : Procrastinator              = _

        def usesInnerInvocations(): Unit = innerInvocations = true

        def withProcrastinator(procrastinator: Procrastinator): Unit = {
            this.procrastinator = procrastinator
        }

        def withRule(rule: RemoteInvocationRule): Unit = rules = Array(rule)

        abstract class params {

            if (usedParams.isEmpty)
                throw new IllegalStateException(s"'params' descriptor of method '${context.javaMethod}' is already set.")
            usedParams = Some(this)

            private val assignements = ListBuffer.empty[As]

            class As(override val tag: Option[Any]) extends Recognizable {

                private[params] var assignedBehavior: ParameterBehavior[AnyRef] = _

                def as(paramName: String): Unit = {
                    assignedBehavior = getParamBehavior(paramName)(s"Parameter '$paramName' not described. Its behavior must be described before.")
                }

                def as(idx: Int): Unit = {
                    assignedBehavior = getParamBehavior(idx)(s"Parameter at index '$idx' not described. Its behavior must be described before.")
                }
            }

            def enable[P](paramName: String, modifier: ParameterModifier[P]): Unit = callOnceContextSet {
                val param = getParam(paramName)
                val bhv = new MethodParameterBehavior[P](param, true, modifier)
            }

            def enable(paramName: String): As = {
                val as = new As(Some(paramName))
                assignements += as
                as
            }

            def enable(idx: Int): As = {
                val as = new As(Some(idx))
                assignements += as
                as
            }

            private[MethodBehaviorBuilder] def concludeAllAssignements(): Unit = {
                for (as <- assignements) {
                    val param = as.tag match {
                        case Some(idx: Int)     => getParam(idx)
                        case Some(name: String) => getParam(name)
                    }
                    val bhv   = as.assignedBehavior
                    paramBehaviors.put(param, new MethodParameterBehavior[AnyRef](param, bhv.isActivated, bhv.modifier))
                }
            }
        }

        private[SynchronizedObjectBehaviorStoreBuilder] def build(): MethodBehavior = {
            usedParams.foreach(_.concludeAllAssignements()) //will modify the paramBehaviors map
            val jMethod            = context.javaMethod
            val parameterBehaviors = jMethod.getParameters.map(getOrDefaultBehavior)
            SyncMethodBehavior(context, parameterBehaviors, parameterBehaviors,
                false, innerInvocations, rules, procrastinator, DefaultMethodInvocationHandler)
        }

        private def getOrDefaultBehavior(parameter: Parameter): ParameterBehavior[AnyRef] = {
            paramBehaviors.getOrElse(parameter, AnnotationBasedMemberBehaviorFactory.genParameterBehavior(parameter))
        }

        private def getParam(paramName: String): Parameter = {
            val method = context.javaMethod
            val params = method.getParameters
            params.find(_.getName == paramName).getOrElse {
                throw new NoSuchElementException(s"No parameter named '$paramName' found for method $method. (method's parameter names are ${params.mkString("[", ", ", "]")})")
            }
        }

        private def getParam(idx: Int): Parameter = {
            context.javaMethod.getParameters()(idx)
        }

        private def getParamBehavior(name: String)(noSuchMsg: String): ParameterBehavior[AnyRef] = {
            paramBehaviors.getOrElse(getParam(name), {
                throw new NoSuchElementException(noSuchMsg)
            })
        }

        private def getParamBehavior(idx: Int)(noSuchMsg: String): ParameterBehavior[AnyRef] = {
            paramBehaviors.getOrElse(getParam(idx), {
                throw new NoSuchElementException(noSuchMsg)
            })
        }

    }

}
