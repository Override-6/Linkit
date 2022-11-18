package fr.linkit.api.gnom.persistence.context

sealed trait ObjectTranform

case class Decomposition(decomposed: Array[Any]) extends ObjectTranform

case class Replaced(replacement: AnyRef) extends ObjectTranform