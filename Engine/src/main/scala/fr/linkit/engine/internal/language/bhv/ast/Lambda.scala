package fr.linkit.engine.internal.language.bhv.ast

trait LambdaKind

case object CurrentToRemoteModifier extends LambdaKind

case object RemoteToCurrentModifier extends LambdaKind

case object CurrentToRemoteEvent extends LambdaKind

case object RemoteToCurrentEvent extends LambdaKind

case class LambdaExpression(block: ScalaCodeBlock, kind: LambdaKind) extends BHVLangToken
    