package fr.linkit.engine.internal.language.bhv.lexer

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

abstract class AbstractTokens {

    lazy final val SymbolsRegex: Regex = symbols0.map(_.regexEscaped).filter(_ != null).mkString("|").r
    private    val keywords0           = ListBuffer.empty[Keyword]
    private    val symbols0            = ListBuffer.empty[Symbol]

    def keywords: Seq[Keyword] = keywords0.toSeq

    def symbols: Seq[Symbol] = symbols0.toSeq

    sealed trait Token extends Product with Serializable {

        val representation: String

        override def toString: String = representation
    }

    abstract class Keyword extends Token {

        override val representation = getClass.getSimpleName.takeWhile(_ ne '$').toLowerCase()
        keywords0 += this
    }

    abstract class Symbol(val symbol: String, val regexEscaped: String = null) extends Token {

        override val representation = symbol
        symbols0 += this
    }

    abstract class Value(override val representation: String) extends Token
}
