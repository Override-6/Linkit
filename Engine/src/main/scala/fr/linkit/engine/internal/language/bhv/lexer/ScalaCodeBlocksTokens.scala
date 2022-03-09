package fr.linkit.engine.internal.language.bhv.lexer

object ScalaCodeBlocksTokens extends AbstractTokens {

    override final val SymbolsRegex = "£\\{|\\{|}|:".r

    case object ValueOpen extends NamedToken("£{")
    case object BracketLeft extends NamedToken("{")
    case object BracketRight extends NamedToken("}")
    case object DoublePoints extends NamedToken(":")
    case object NewLine extends NamedToken("\\n")

    case class CodeFragment(code: String) extends NamedToken
    case class Identifier(str: String) extends NamedToken(s"Identifier($str)")
}
