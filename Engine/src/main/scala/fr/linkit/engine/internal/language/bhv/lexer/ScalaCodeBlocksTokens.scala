package fr.linkit.engine.internal.language.bhv.lexer

object ScalaCodeBlocksTokens extends AbstractTokens {
    case object ValueOpen extends Symbol("£{")
    case object ValueClose extends Symbol("}")
    case object DoublePoints extends Symbol(":")

    case class CodeFragment(code: String) extends Value(code)
    case class Identifier(str: String) extends Value(s"Identifier($str)")
}
