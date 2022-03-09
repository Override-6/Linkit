package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex

abstract class AbstractTokens {

    val SymbolsRegex: Regex

    sealed trait Token extends Product with Serializable {

        val name: Option[String]

        override def toString: String = name.getOrElse(getClass.getSimpleName.toLowerCase().dropRight(1))
    }

    abstract class NamedToken(override val name: Option[String]) extends Token {

        def this() = this(None)

        def this(name: String) = this(Option(name))
    }

}
