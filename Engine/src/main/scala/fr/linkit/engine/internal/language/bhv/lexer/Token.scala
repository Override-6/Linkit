package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex

trait Token {
    val representation: String

    override def toString: String = representation
}

trait Keyword extends Token
trait Symbol extends Token

object Symbol {
    def makeRegex(tokens: Array[Symbol]): Regex = {
       tokens.map(_.representation.map("\\" + _).mkString(""))
               .mkString("|").r
    }
}

abstract class Value(override val representation: String) extends Token
