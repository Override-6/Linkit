package fr.linkit.engine.internal.language.bhv.ast

trait MethodDescription

trait AttributedMethodDescription extends MethodDescription {

    val signature: MethodSignature
}

class DisabledMethodDescription() extends MethodDescription

class HiddenMethodDescription(val hideMessage: Option[String]) extends MethodDescription

class EnabledMethodDescription(val referent: Option[ExternalReference],
                               val procrastinatorName: Option[ExternalReference],
                               val modifiers: Seq[MethodComponentsModifier],
                               val syncReturnValue: SynchronizeState) extends MethodDescription

object EnabledMethodDescription {

    def apply(referent: Option[ExternalReference],
              procrastinatorName: Option[ExternalReference],
              modifiers: Seq[MethodComponentsModifier],
              syncReturnValue: SynchronizeState)(sig: MethodSignature): EnabledMethodDescription with AttributedMethodDescription = {
        new EnabledMethodDescription(referent, procrastinatorName, modifiers, syncReturnValue) with AttributedMethodDescription {
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

case class MethodParam(synchronized: SynchronizeState, tpe: String) {

    override def toString: String = (if (synchronized.isSync) "synchronized " else "") + tpe
}

case class MethodSignature(methodName: String, params: Seq[MethodParam]) {

    override def toString: String = s"$methodName${params.mkString("(", ",", ")")}"
}

case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression])
