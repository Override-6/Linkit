package fr.linkit.engine.internal.language.bhv.ast

trait MethodDescription

trait AttributedMethodDescription extends MethodDescription {

    val signature: MethodSignature
}

trait AttributedEnabledMethodDescription extends AttributedMethodDescription {

    val modifiers: List[CompModifier]
}

class DisabledMethodDescription() extends MethodDescription

class HiddenMethodDescription(val hideMessage: Option[String]) extends MethodDescription

class EnabledMethodDescription(val properties: List[MethodProperty],
                               val agreement: Option[AgreementReference],
                               val syncReturnValue: SynchronizeState) extends MethodDescription

object EnabledMethodDescription {

    def apply(properties: List[MethodProperty],
              agreement: Option[AgreementReference],
              syncReturnValue: SynchronizeState)
             (sig: MethodSignature, mods: List[CompModifier]): EnabledMethodDescription with AttributedMethodDescription = {
        new EnabledMethodDescription(properties, agreement, syncReturnValue) with AttributedEnabledMethodDescription {
            override val signature = sig
            override val modifiers = mods
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

case class MethodParam(syncState: SynchronizeState, name: Option[String], tpe: String) {

    override def toString: String = (if (syncState.isSync) "sync " else "") + tpe
}

case class MethodSignature(methodName: String, params: Seq[MethodParam]) {

    override def toString: String = s"$methodName${params.mkString("(", ",", ")")}"
}

case class MethodComponentsModifier(paramsModifiers: Map[Int, CompModifier], rvModifiers: Seq[CompModifier])

trait CompModifier {
    val target: String
}

case class ValueModifierReference(target: String, ref: String) extends CompModifier

case class ModifierExpression(target: String, in: Option[LambdaExpression], out: Option[LambdaExpression]) extends CompModifier