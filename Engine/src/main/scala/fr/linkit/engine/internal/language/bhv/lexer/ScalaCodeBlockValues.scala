package fr.linkit.engine.internal.language.bhv.lexer

object ScalaCodeBlockValues {
    case class CodeFragment(code: String) extends Value(code) with ScalaCodeBlockToken

    case class Identifier(str: String) extends Value(s"Identifier($str)") with ScalaCodeBlockToken
}