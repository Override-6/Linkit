package fr.linkit.engine.internal.language.bhv.ast

case class TypeModifier(typeName: String, in: Option[LambdaExpression], out: Option[LambdaExpression]) extends LambdaExpressionHolder

case class ValueModifier(name: String, typeName: String, in: Option[LambdaExpression], out: Option[LambdaExpression]) extends LambdaExpressionHolder
