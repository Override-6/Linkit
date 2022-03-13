package fr.linkit.engine.internal.language.bhv.ast

case class TypeModifier(className: String, in: Option[LambdaExpression], out: Option[LambdaExpression])

case class ValueModifier(name: String, tpeName: String, in: Option[LambdaExpression], out: Option[LambdaExpression])
