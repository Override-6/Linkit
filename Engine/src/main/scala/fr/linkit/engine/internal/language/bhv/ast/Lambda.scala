package fr.linkit.engine.internal.language.bhv.ast

trait LambdaKind

case object In extends LambdaKind

case object Out extends LambdaKind

case class LambdaExpression(block: ScalaCodeBlock, kind: LambdaKind)
    