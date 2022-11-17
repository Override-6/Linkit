/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod

trait MethodDescription

trait AttributedMethodDescription extends MethodDescription {

    val signature: MethodSignature
}

trait AttributedEnabledMethodDescription extends AttributedMethodDescription {

   // val modifiers: List[CompModifier]
}

class DisabledMethodDescription() extends MethodDescription

class HiddenMethodDescription(val hideMessage: Option[String]) extends MethodDescription

class EnabledMethodDescription(val invocationHandlingMethod: InvocationHandlingMethod,
                               val properties              : List[MethodProperty],
                               val agreementOpt            : Option[AgreementProvider],
                               val syncReturnValue         : RegistrationState) extends MethodDescription

object EnabledMethodDescription {

    def apply(invocationHandlingMethod: InvocationHandlingMethod,
              properties: List[MethodProperty],
              dispatch: Option[AgreementProvider])
             (sig: MethodSignature): EnabledMethodDescription with AttributedMethodDescription = {
        val rvState = sig.returnType.map(_.syncType).getOrElse(RegistrationState(false, SyncLevel.NotRegistered))
        new EnabledMethodDescription(invocationHandlingMethod, properties, dispatch, rvState) with AttributedEnabledMethodDescription {
            override val signature = sig
        }
    }
}

object HiddenMethodDescription {

    def apply(hideMessage: Option[String])(sig: MethodSignature): HiddenMethodDescription with AttributedMethodDescription = {
        new HiddenMethodDescription(hideMessage) with AttributedMethodDescription {
            override val signature: MethodSignature = sig
        }
    }
}

case object DisabledMethodDescription {

    def apply(sig: MethodSignature): DisabledMethodDescription with AttributedMethodDescription = {
        new DisabledMethodDescription() with AttributedMethodDescription {
            override val signature: MethodSignature = sig
        }
    }
}

case class MethodProperty(name: String, value: String)

case class SynchronizedType(syncType: RegistrationState, tpe: Option[String]) {
    override def toString: String = syncType.lvl.name().toLowerCase() + tpe.map(" " + _).getOrElse("")
}

case class MethodParam(name: Option[String], tpe: SynchronizedType) {

    override def toString: String = (tpe.syncType.lvl match {
        case SyncLevel.NotRegistered => ""
        case SyncLevel.Chipped       => "chip "
        case SyncLevel.Synchronized  => "sync "
    }) + name.map(n => s"$n: ").getOrElse("") + tpe.tpe.get
}

case class MethodSignature(target: Option[String], methodName: String, params: Seq[MethodParam], returnType: Option[SynchronizedType]) {

    override def toString: String = {
        target.map(_ + ".").getOrElse("") +
                s"$methodName${params.mkString("(", ",", ")")}" +
                returnType.map(rt => s": $rt").getOrElse("")
    }
}

//case class MethodComponentsModifier(paramsModifiers: Map[Int, CompModifier], rvModifiers: Seq[CompModifier])
/*
trait CompModifier {
    
    val target: String
}
*/
/*
case class ValueModifierReference(target: String, ref: String) extends CompModifier

case class ModifierExpression(target: String, in: Option[LambdaExpression], out: Option[LambdaExpression]) extends CompModifier with LambdaExpressionHolder*/