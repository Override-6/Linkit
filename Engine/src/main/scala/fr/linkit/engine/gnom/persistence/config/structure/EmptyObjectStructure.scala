package fr.linkit.engine.gnom.persistence.config.structure

import fr.linkit.api.gnom.persistence.obj.ObjectStructure

object EmptyObjectStructure extends ObjectStructure {

    override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = from == to

    override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = from == to
}
