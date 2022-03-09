package fr.linkit.engine.test

import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.ScalaCodeBlocksParser
import org.junit.jupiter.api.Test

import scala.util.parsing.input.CharSequenceReader

class BehaviorLangParserTests {

    @Test
    def parse(): Unit = {
        //Contract("/contracts/NetworkContract.bhv")

    }

    @Test
    def lex(): Unit = {
        val file = "/contracts/NetworkContract.bhv"
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val in = new CharSequenceReader(source)
        val tokens = BehaviorLanguageLexer.tokenize(in)
        print(tokens)
        tokens
    }

    @Test
    def lexScalaBlock(): Unit = {
        val file = "/contracts/NetworkContract.bhv"
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val in = new CharSequenceReader(source)
        val tokens = ScalaCodeBlocksParser.parse(in)
        print(tokens)
        tokens
    }

}
