package fr.linkit.engine.connection.cache.`object`.description.language.parser

import scala.util.parsing.combinator.Parsers

class TestParser extends Parsers {

    val expr: Parser[Any] = number ~ opt(operator )

}
