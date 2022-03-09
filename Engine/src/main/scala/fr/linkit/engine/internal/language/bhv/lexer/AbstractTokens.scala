package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex

abstract class AbstractTokens {

    val SymbolsRegex: Regex

    sealed trait Token {
        val name: Option[String]
        override def toString: String = name.getOrElse(getClass.getSimpleName.toLowerCase().dropRight(1))
    }
    class NamedToken (override val name: Option[String]) extends Token {
        def this() = this(None)
        def this(name: String) = this(Option(name))
    }

}
