package fr.linkit.engine.internal.language.bhv.lexer

import scala.util.matching.Regex

trait Token {
    val value: String

    override def toString: String = value
}

trait Keyword extends Token
trait Symbol extends Token

object Symbol {
    def makeRegex(tokens: Array[Symbol]): Regex = {
       tokens.map(_.value.map("\\" + _).mkString(""))
               .mkString("|").r
    }
}

abstract class Value(override val value: String) extends Token
