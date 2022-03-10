package fr.linkit.engine.internal.language.bhv.ast

case class FieldDescription(state: SynchronizeState)

case class AttributedFieldDescription(fieldName: Option[String], state: SynchronizeState)