package fr.linkit.engine.internal.language.bhv.lexer

object ScalaCodeBlocksTokens extends AbstractTokens {

    override final val SymbolsRegex = "£\\{|:|}".r

    case object ValueOpen extends NamedToken("£{")
    case object ValueClose extends NamedToken("}")
    case object DoublePoints extends NamedToken(":")

    case class CodeFragment(code: String) extends NamedToken(code)
    case class Identifier(str: String) extends NamedToken(s"Identifier($str)")
}
