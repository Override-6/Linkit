package fr.linkit.engine.internal.language.bhv.ast

sealed trait Reference {
    val ref: String
}

case class ExternalReference(ref: String) extends Reference