package fr.linkit.engine.internal.language.bhv.ast

class FieldDescription(val state: RegistrationState)

case class AttributedFieldDescription(targetClass: Option[String], fieldName: String,
                                      private val syncState: RegistrationState) extends FieldDescription(syncState)