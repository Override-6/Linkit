package fr.linkit.engine.internal.language.bhv.lexer

import fr.linkit.engine.internal.language.bhv.lexer
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Position

abstract class AbstractLexer extends RegexParsers {
    
    protected def whitespaceChar = "\\s".r
    
    override final protected val whiteSpace: Regex = {
        val ws = whitespaceChar.regex
        val r  = (ws + "*(((//.*\\n?)|(/\\*[\\s\\S]+?\\*/))" + ws + "*)|" + ws + "+").r
        r
    }
    
    type Token <: lexer.Token
    type PosToken = (Token, Position)
    
    protected val symbols : Array[_ <: Token with Symbol]
    protected val keywords: Array[_ <: Token with Keyword]
    
    protected def symbolsRegex: Regex
    
    protected lazy val keywordParser   : Parser[PosToken] = {
        val keywords = this.keywords.map(x => (x.value, x)).toMap
        pos("[\\w`]+".r.filter(s => s.head != '`' && s.last != '`')
                    .filter(keywords.contains) ^^ keywords.apply)
    }
    protected lazy val symbolParser    : Parser[PosToken] = {
        val symbols = this.symbols.map(x => (x.value, x)).toMap
        pos(symbolsRegex.filter(symbols.contains) ^^ symbols)
    }
    protected lazy val identifierParser: Parser[String]   = {
        val s = symbols.map(_.value.map("\\" + (_: Char)).mkString("")).mkString("")
        s"`[^\\s$s]+`".r.map(s => s.slice(1, s.length - 1)) | s"[^\\s$s]+".r
    }
    
    override protected def handleWhiteSpace(source: CharSequence, offset: Int): Int = {
        if (skipWhitespace) {
            whiteSpace.findPrefixMatchOf(new SubSequence(source, offset)) match {
                case Some(matched) =>
                    handleWhiteSpace(source, offset + matched.end)
                case None          =>
                    offset
            }
        } else offset
    }
    
    implicit def pos[P <: Token](parser: Parser[P]): Parser[(P, Position)] = {
        Parser { in =>
            parser(in) match {
                case Failure(msg, in) => Failure(msg, in)
                case Error(msg, in)   => Error(msg, in)
                case Success(x, next) => Success((x, in.pos), next)
            }
        }
    }
}
