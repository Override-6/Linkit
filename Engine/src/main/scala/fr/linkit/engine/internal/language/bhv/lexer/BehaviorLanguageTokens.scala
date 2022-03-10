package fr.linkit.engine.internal.language.bhv.lexer

object BehaviorLanguageTokens extends AbstractTokens {

    case object Import extends Keyword
    case object Describe extends Keyword
    case object Method extends Keyword
    case object Scala extends Keyword
    case object Enable extends Keyword
    case object Stub extends Keyword
    case object Mirroring extends Keyword
    case object Disable extends Keyword
    case object Statics extends Keyword
    case object Hide extends Keyword
    case object As extends Keyword
    case object Modifier extends Keyword
    case object Synchronize extends Keyword
    case object ReturnValue extends Keyword

    case object Not extends Symbol("!")
    case object And extends Symbol("&")
    case object Equal extends Symbol("=")
    case object BracketLeft extends Symbol("{", "\\{")
    case object BracketRight extends Symbol("}")
    case object SquareBracketLeft extends Symbol("[", "\\[")
    case object SquareBracketRight extends Symbol("]")
    case object Arrow extends Symbol("->")
    case object Comma extends Symbol(",")
    case object ParenRight extends Symbol(")", "\\)")
    case object ParenLeft extends Symbol("(", "\\(")

    case class CodeBlock(sourceCode: String) extends Value(s"$${${sourceCode.dropRight(1)}}")
    case class Identifier(str: String) extends Value(s"Identifier($str)")
    case class Literal(str: String) extends Value(s"Literal(\"$str\")")

}
