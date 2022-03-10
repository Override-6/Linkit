package fr.linkit.engine.internal.language.bhv.ast

trait MethodDescription

trait AttributedMethodDescription extends MethodDescription {
    val signature: MethodSignature
}

case class DisabledMethodDescription() extends MethodDescription

case class HiddenMethodDescription(hideMessage: Option[String]) extends MethodDescription

case class EnabledMethodDescription(referent: Option[InternalReference],
                                    procrastinatorName: Option[ExternalReference],
                                    modifiers: Seq[MethodComponentsModifier],
                                    syncReturnValue: SynchronizeState) extends MethodDescription

case class MethodParam(synchronized: SynchronizeState, tpe: String) {
    override def toString: String = (if (synchronized.isSync) "synchronized " else "") + tpe
}

case class MethodSignature(methodName: String, params: Seq[MethodParam]) {
    override def toString: String = s"$methodName${params.mkString("(", ",", ")")}"
}

case class MethodComponentsModifier(paramsModifiers: Map[Int, Seq[LambdaExpression]], returnvalueModifiers: Seq[LambdaExpression])
