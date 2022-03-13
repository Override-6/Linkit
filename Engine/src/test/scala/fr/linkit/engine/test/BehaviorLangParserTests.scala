package fr.linkit.engine.test

import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.Contract
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.ScalaCodeBlocksParser
import org.junit.jupiter.api.Test

import scala.util.parsing.input.CharSequenceReader

class BehaviorLangParserTests {

    @Test
    def parse(): Unit = {
        Contract("contracts/NetworkContract.bhv")(null)
    }

    @Test
    def lex(): Unit = {
        val file   = "contracts/NetworkContract.bhv"
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val in     = new CharSequenceReader(source)
        val tokens = BehaviorLanguageLexer.tokenize(in, file)
        print(tokens)
        tokens
    }

    @Test
    def lexScalaBlock(): Unit = {
        val file   = "/contracts/NetworkContract.bhv"
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val in     = new CharSequenceReader(source)
        val tokens = ScalaCodeBlocksParser.parse(in)
        print(tokens)
        tokens
    }

}
