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

package fr.linkit.engine.gnom.cache.sync.contract.behavior.builder

import fr.linkit.api.gnom.cache.sync.contract.ParameterContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.gnom.cache.sync.contract.behavior.{RemoteInvocationRule, SynchronizedObjectContractFactory}
import fr.linkit.api.gnom.cache.sync.contract.description.{FieldDescription, MethodDescription, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifier
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.gnom.cache.sync.contract.MethodParameterContract
import fr.linkit.engine.gnom.cache.sync.contract.behavior.builder.SynchronizedObjectBehaviorFactoryBuilder.{MethodBehaviorBuilder, Recognizable}
import fr.linkit.engine.gnom.cache.sync.contract.behavior.member.{MethodParameterBehavior, MethodReturnValueBehavior, SyncFieldBehavior}
import fr.linkit.engine.gnom.cache.sync.contract.behavior.{AnnotationBasedMemberBehaviorFactory, SyncObjectContractFactory}
import fr.linkit.engine.gnom.cache.sync.invokation.DefaultMethodInvocationHandler

import java.lang.reflect.{Field, Method, Parameter}
import java.util.NoSuchElementException
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class SynchronizedObjectBehaviorFactoryBuilder {

    private val builders       = mutable.LinkedHashMap.empty[Any, ClassDescriptor[_]]
    private var defaultID: Int = 0

    def describe[C <: AnyRef](builder: ClassDescriptor[C]): Unit = {
        val tag = builder.tag.getOrElse(nextDescriptorDefaultID)
        builders.put(tag, builder)
    }

    def build(): SynchronizedObjectContractFactory = {
        var descriptions = builders.values.map(_.getResult).toArray
        if (!descriptions.exists(_.targetClass eq classOf[Object])) {
            descriptions :+= new ObjectBehaviorDescriptor[Object] {
                override val targetClass          : Class[Object]                                = classOf[Object]
                override val usingHierarchy       : Array[ObjectBehaviorDescriptor[_ >: Object]] = Array.empty
                override val withMethods          : Array[MethodContractDescriptor]              = Array.empty
                override val withFields           : Array[FieldBehavior[Any]]                    = Array.empty
                override val whenField            : Option[FieldModifier[Object]]                = None
                override val whenParameter        : Option[MethodCompModifier[Object]]           = None
                override val whenMethodReturnValue: Option[MethodCompModifier[Object]]           = None
            }
        }
        new SyncObjectContractFactory(descriptions)
    }

    private def nextDescriptorDefaultID: Int = {
        defaultID += 1
        defaultID
    }

    abstract class ClassDescriptor[T <: AnyRef](override val tag: Option[Any])(implicit desc: SyncStructureDescription[T]) extends Recognizable {

        def this()(implicit desc: SyncStructureDescription[T]) {
            this(None)(desc)
        }

        private val clazz                  = desc.clazz
        private val methodBehaviors        = mutable.HashMap.empty[Method, MethodContractDescriptor]
        private val fieldBehaviors         = mutable.HashMap.empty[Field, SyncFieldBehavior[Any]]
        private val inheritedBehaviorsTags = ListBuffer.empty[Any]

        protected var whenField            : FieldModifier[T]            = _
        protected var whenParameter        : MethodCompModifier[T]       = _
        protected var whenMethodReturnValue: MethodCompModifier[T]       = _
        private   var result               : ObjectBehaviorDescriptor[_] = _

        def inherit(tags: Any*): Unit = {
            inheritedBehaviorsTags ++= tags
        }

        def enable: MemberEnable = new MemberEnable

        def disable: MemberDisable = new MemberDisable

        class MemberDisable {

            def field(name: String): Unit = {
                val fDesc = desc.findFieldDescription(name).get
                fieldBehaviors.put(fDesc.javaField, SyncFieldBehavior(fDesc, false, null))
            }

            def method(name: String): Unit = {
                val mDesc              = desc.findMethodDescription(name).get
                val contractDescriptor = new MethodContractDescriptor(mDesc, BasicInvocationRule.ONLY_CURRENT, false)
                methodBehaviors.put(mDesc.javaMethod, contractDescriptor)
            }
        }

        class MemberEnable {

            class MethodBehaviorDescriptorBuilderIntroduction(descs: Array[MethodDescription]) {

                private var concluded = false

                def and(methodName: String): MethodBehaviorDescriptorBuilderIntroduction = {
                    new MethodBehaviorDescriptorBuilderIntroduction(descs :+ getMethod(methodName))
                }

                def as(methodName: String): Unit = conclude {
                    val bhv = methodBehaviors.getOrElse(getMethod(methodName).javaMethod, {
                        throw new NoSuchElementException(s"Method '$methodName' not described. its behavior must be described before.")
                    })
                    descs.map(new MethodContractDescriptor(_, bhv))
                }

                def as(builder: MethodBehaviorBuilder): Unit = conclude {
                    descs.map(desc => {
                        builder.setContext(desc)
                        builder.build()
                    })
                }

                def withRule(rule: RemoteInvocationRule): Unit = conclude {
                    descs.map(desc => {
                        new MethodContractDescriptor(desc, rule, true)
                    })
                }

                private def conclude(conclusion: => Array[MethodContractDescriptor]): Unit = {
                    if (concluded)
                        throw new IllegalStateException("This method was already described.")
                    conclusion.foreach { contractDescriptor =>
                        methodBehaviors.put(contractDescriptor.description.javaMethod, contractDescriptor)
                    }
                    concluded = true
                }

            }

            class FieldBehaviorDescriptorBuilderIntroduction(descs: Array[FieldDescription]) {
                //TODO be able to build field behaviors
            }

            private def getMethod(name: String): MethodDescription = {
                desc.findMethodDescription(name).getOrElse {
                    throw new NoSuchElementException(s"Can not find declared or inherited method '$name' in $clazz, is this method final or private ?")
                }
            }

            private def getField(name: String): FieldDescription = {
                desc.findFieldDescription(name).getOrElse {
                    throw new NoSuchElementException(s"Can not find declared or inherited field '$name' in $clazz")
                }
            }

            def method(name: String): MethodBehaviorDescriptorBuilderIntroduction = {
                val mDesc = getMethod(name)
                new MethodBehaviorDescriptorBuilderIntroduction(Array(mDesc))
            }

            def field[F <: AnyRef](name: String)(modifier: FieldModifier[F]): Unit = {
                val fDesc = getField(name)
                val bhv   = new SyncFieldBehavior[F](fDesc, true, modifier)
                fieldBehaviors.put(fDesc.javaField, bhv.asInstanceOf[SyncFieldBehavior[Any]])
            }
        }

        private[SynchronizedObjectBehaviorFactoryBuilder] def getResult: ObjectBehaviorDescriptor[_] = {
            if (result != null)
                return result
            val hierarchy = inheritedBehaviorsTags.map(tag => builders.getOrElse(tag, {
                throw new NoSuchElementException(s"Could not find behavior descriptor with tag '$tag'.")
            }).getResult).toArray.asInstanceOf[Array[ObjectBehaviorDescriptor[_ >: T]]]

            val builder = this
            result = new ObjectBehaviorDescriptor[T] {
                override val targetClass   : Class[T]                                = desc.clazz
                override val usingHierarchy: Array[ObjectBehaviorDescriptor[_ >: T]] = hierarchy
                override val withMethods   : Array[MethodContractDescriptor]         = methodBehaviors.values.toArray
                override val withFields    : Array[FieldBehavior[Any]]               = fieldBehaviors.values.toArray

                override val whenField            : Option[FieldModifier[T]]      = Option(builder.whenField)
                override val whenParameter        : Option[MethodCompModifier[T]] = Option(builder.whenParameter)
                override val whenMethodReturnValue: Option[MethodCompModifier[T]] = Option(builder.whenMethodReturnValue)
            }
            result
        }
    }

}

object SynchronizedObjectBehaviorFactoryBuilder {

    sealed trait Recognizable {

        val tag: Option[Any]
    }

    abstract class MethodBehaviorBuilder(rule: RemoteInvocationRule = BasicInvocationRule.ONLY_CURRENT) extends AbstractBehaviorBuilder[MethodDescription] {

        private val paramBehaviors             = mutable.HashMap.empty[Parameter, ParameterContract[Any]]
        private var usedParams: Option[params] = None

        private var forceLocalInvocation: Boolean        = false
        private var procrastinator      : Procrastinator = _

        def mustForceLocalInvocation(): Unit = forceLocalInvocation = true

        def withProcrastinator(procrastinator: Procrastinator): Unit = {
            this.procrastinator = procrastinator
        }

        //def withRule(rule: RemoteInvocationRule): Unit = rules = Array(rule)

        object returnvalue {

            var enabled = true
            private[MethodBehaviorBuilder] var modifier: MethodCompModifier[_] = _

            def withModifier[R <: AnyRef](modifier: MethodCompModifier[R]): Unit = {
                if (this.modifier != null)
                    throw new IllegalStateException("Return Value modifier already set !")
                this.modifier = modifier
            }
        }

        abstract class params {

            if (usedParams.isDefined)
                throw new IllegalStateException(s"'params' descriptor is already set.")
            usedParams = Some(this)

            private val assignements = ListBuffer.empty[As]

            class As(override val tag: Option[Any]) extends Recognizable {

                private[params] var assignedBehavior: ParameterContract[Any] = _

                def as(paramName: String): Unit = {
                    assignedBehavior = getParamBehavior(paramName)(s"Parameter '$paramName' not described. Its behavior must be described before.")
                }

                def as(idx: Int): Unit = {
                    assignedBehavior = getParamBehavior(idx)(s"Parameter at index '$idx' not described. Its behavior must be described before.")
                }
            }

            def enable[P <: AnyRef](paramName: String)(paramModifier: MethodCompModifier[P]): Unit = callOnceContextSet {
                val param = getParam(paramName)
                val bhv   = new MethodParameterContract[P](param, Some(MethodParameterBehavior(true)), Option(paramModifier))
                paramBehaviors.put(param, bhv.asInstanceOf[ParameterContract[Any]])
            }

            def enable[P <: AnyRef](idx: Int, paramModifier: MethodCompModifier[P]): Unit = callOnceContextSet {
                val param = getParam(idx)
                val bhv   = new MethodParameterContract[P](param, Some(MethodParameterBehavior(true)), Option(paramModifier))
                paramBehaviors.put(param, bhv.asInstanceOf[ParameterContract[Any]])
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
                    paramBehaviors.put(param, bhv)
                }
            }
        }

        private[SynchronizedObjectBehaviorFactoryBuilder] def build(): MethodContractDescriptor = {
            usedParams.foreach(_.concludeAllAssignements()) //will modify the paramBehaviors map
            val jMethod             = context.javaMethod
            val parameterBehaviors  = getParamContracts(jMethod)
            val returnValueBehavior = new MethodReturnValueBehavior[Any](returnvalue.enabled)
            val returnValueModifier = returnvalue.modifier.asInstanceOf[MethodCompModifier[Any]]
            MethodContractDescriptor(
                context, rule, true, parameterBehaviors,
                returnValueBehavior, returnValueModifier, false, forceLocalInvocation,
                procrastinator, DefaultMethodInvocationHandler)
        }

        private def getParamContracts(jMethod: Method): Array[ParameterContract[Any]] = {
            val base = jMethod.getParameters.map(getOrDefaultContract)
            if (base.exists(pb => pb.modifier != null || pb.behavior.exists(_.isActivated)))
                base
            else Array.empty
        }

        private def getOrDefaultContract(parameter: Parameter): ParameterContract[Any] = {
            paramBehaviors.getOrElse(parameter, AnnotationBasedMemberBehaviorFactory.genParameterBehavior(parameter))
                    .asInstanceOf[ParameterContract[Any]]
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

        private def getParamBehavior(name: String)(noSuchMsg: String): ParameterContract[Any] = {
            val param = getParam(name)
            if (param.getType.isPrimitive)
                throw new UnsupportedOperationException("can't synchronize or apply modifiers on primitive values.")
            paramBehaviors.getOrElse(param, {
                throw new NoSuchElementException(noSuchMsg)
            })
        }

        private def getParamBehavior(idx: Int)(noSuchMsg: String): ParameterContract[Any] = {
            val param = getParam(idx)
            if (param.getType.isPrimitive)
                throw new UnsupportedOperationException("can't synchronize or apply modifiers on primitive values.")
            paramBehaviors.getOrElse(param, {
                throw new NoSuchElementException(noSuchMsg)
            })
        }

    }

}
