package fr.linkit.engine.internal.language.bhv.ast

class FieldDescription(val state: RegistrationState)

case class AttributedFieldDescription(fieldName: String,
                                      private val syncState: RegistrationState) extends FieldDescription(syncState)