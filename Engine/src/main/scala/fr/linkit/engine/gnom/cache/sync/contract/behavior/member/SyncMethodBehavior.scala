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

package fr.linkit.engine.gnom.cache.sync.contract.behavior.member

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifierKind
import fr.linkit.api.gnom.cache.sync.contract.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder, RemoteInvocationRule, SynchronizedObjectBehaviorFactory}
import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.remote.{MethodInvocationHandler, Puppeteer}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class SyncMethodBehavior(override val desc: MethodDescription,
                              override val parameterBehaviors: Array[ParameterBehavior[AnyRef]],
                              @Nullable override val returnValueBehavior: ReturnValueBehavior[AnyRef],
                              override val isHidden: Boolean,
                              override val forceLocalInnerInvocations: Boolean,
                              private val rules: Array[RemoteInvocationRule],
                              @Nullable override val procrastinator: Procrastinator,
                              @Nullable override val handler: MethodInvocationHandler) extends InternalMethodBehavior {

    override def getName: String = desc.javaMethod.getName

    /**
     * @return true if the method can perform an RMI
     * */
    override val isActivated: Boolean = handler != null

    private val usesRemoteParametersModifier: Boolean = parameterBehaviors.exists(_ != null)

    /**
     * The default return value of the method, based on
     * [[https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html the default primitives value]].
     * */
    override lazy val defaultReturnValue: Any = {
        val clazz = desc.javaMethod.getReturnType
        if (clazz.isPrimitive) {
            import java.{lang => l}
            clazz match {
                case l.Character.TYPE => '\u0000'
                case l.Boolean.TYPE   => false
                case _                => 0
            }
        } else {
            null
        }
    }

    override def completeAgreement(agreement: RMIRulesAgreementBuilder): RMIRulesAgreement = {
        rules.foreach(_.apply(agreement))
        agreement.result
    }

    /**
     * uses the given RMIDispatcher
     *
     * @param dispatcher the dispatcher to use
     */
    override def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, factory: SynchronizedObjectBehaviorFactory, invocation: LocalMethodInvocation[_]): Unit = {
        val args = invocation.methodArguments
        if (!usesRemoteParametersModifier) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            val engine = network.findEngine(engineID).get
            for (i <- args.indices) {
                val paramBehavior = parameterBehaviors(i)
                val paramTpe      = paramBehavior.param.getType.asInstanceOf[Class[_ >: AnyRef]]
                val modifier = paramBehavior.modifier
                args(i) match {
                    case ref: AnyRef =>
                        var finalRef = ref
                        if (modifier != null) {
                            finalRef = modifier.toRemote(finalRef, invocation, engine)
                            modifier.toRemoteEvent(finalRef, invocation, engine)
                        }
                        finalRef = finalRef match {
                            case sync: SynchronizedObject[AnyRef] =>
                                sync.getBehavior.multiModifier.modifyForParameter(sync, paramTpe)(invocation, engine, MethodCompModifierKind.TO_REMOTE)
                            case x                                => x
                        }
                        buff(i) = finalRef
                    //impossible due to boxing, but keep it logical:
                    // it's impossible to synchronize / convert primitive values,
                    // so the sync object system can't apply on primitives
                    case _ =>
                }
            }
            buff
        })
    }
}

object SyncMethodBehavior {

    def copy(desc: MethodDescription, other: SyncMethodBehavior): SyncMethodBehavior = {
        SyncMethodBehavior(
            desc, other.parameterBehaviors, other.returnValueBehavior,
            other.isHidden, other.forceLocalInnerInvocations, other.rules,
            other.procrastinator, other.handler)
    }

    def disabled(desc: MethodDescription): SyncMethodBehavior = {
        SyncMethodBehavior(desc, Array.empty, null, false, false, Array.empty, null, null)
    }
}