package fr.linkit.engine.internal.language.bhv.ast

sealed trait Reference {
    val ref: String
}

case class InternalReference(ref: String) extends Reference

case class ExternalReference(ref: String) extends Reference