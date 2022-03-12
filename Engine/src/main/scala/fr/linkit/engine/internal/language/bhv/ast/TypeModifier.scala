package fr.linkit.engine.internal.language.bhv.ast

case class TypeModifier(className: String, modifiers: Seq[LambdaExpression])

case class ValueModifier(name: String, tpeName: String, modifiers: Seq[LambdaExpression])
