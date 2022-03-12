package fr.linkit.engine.internal.language.bhv.lexer

object BehaviorLanguageValues {

    case class CodeBlock(sourceCode: String) extends Value(s"$${${sourceCode.dropRight(1)}}") with BehaviorLanguageToken
    case class Identifier(str: String) extends Value(s"Identifier($str)") with BehaviorLanguageToken
    case class Literal(str: String) extends Value(s"Literal(\"$str\")") with BehaviorLanguageToken

}
