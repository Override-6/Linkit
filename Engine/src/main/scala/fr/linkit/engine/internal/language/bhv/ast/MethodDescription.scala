package fr.linkit.engine.internal.language.bhv.ast

trait MethodDescription

trait AttributedMethodDescription extends MethodDescription {

    val signature: MethodSignature
}

class DisabledMethodDescription() extends MethodDescription

class HiddenMethodDescription(val hideMessage: Option[String]) extends MethodDescription

class EnabledMethodDescription(val properties: List[MethodProperty],
                               val referent: Option[ExternalReference],
                               val procrastinatorName: Option[ExternalReference],
                               val modifiers: List[CompModifier],
                               val syncReturnValue: SynchronizeState) extends MethodDescription

object EnabledMethodDescription {

    def apply(properties: List[MethodProperty],
              referent: Option[ExternalReference],
              procrastinatorName: Option[ExternalReference],
              modifiers: List[CompModifier],
              syncReturnValue: SynchronizeState)(sig: MethodSignature): EnabledMethodDescription with AttributedMethodDescription = {
        new EnabledMethodDescription(properties, referent, procrastinatorName, modifiers, syncReturnValue) with AttributedMethodDescription {
            override val signature: MethodSignature = sig
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

case object DisabledMethodDescription extends DisabledMethodDescription {

    def apply(sig: MethodSignature): DisabledMethodDescription with AttributedMethodDescription = {
        new DisabledMethodDescription() with AttributedMethodDescription {
            override val signature: MethodSignature = sig
        }
    }
}

case class MethodProperty(name: String, value: String)

case class MethodParam(synchronized: SynchronizeState, name: Option[String], tpe: String) {

    override def toString: String = (if (synchronized.isSync) "sync " else "") + tpe
}

case class MethodSignature(methodName: String, params: Seq[MethodParam]) {

    override def toString: String = s"$methodName${params.mkString("(", ",", ")")}"
}

case class MethodComponentsModifier(paramsModifiers: Map[Int, CompModifier], rvModifiers: Seq[CompModifier])

trait CompModifier

case class ValueModifierReference(target: String, ref: String) extends CompModifier

case class ModifierExpression(target: String, in: Option[LambdaExpression], out: Option[LambdaExpression]) extends CompModifier