package fr.linkit.engine.internal.language.bhv.ast

class FieldDescription(val state: SynchronizeState)

case class AttributedFieldDescription(fieldName: String,
                                      private val state: SynchronizeState) extends FieldDescription(state)