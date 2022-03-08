package fr.linkit.engine.internal.language.bhv.lexer

object BehaviorLanguageTokens {

    final val SymbolsRegex = "}|]|->|\\[|\\{|,|\\)|\\(".r

    sealed trait Token {
        val name: Option[String]
        override def toString: String = name.getOrElse(getClass.getSimpleName.toLowerCase().dropRight(1))
    }
    class NamedToken (override val name: Option[String]) extends Token {
        def this() = this(None)
        def this(name: String) = this(Option(name))
    }
    case object Import extends NamedToken
    case object Describe extends NamedToken
    case object Method extends NamedToken
    case object Class extends NamedToken
    case object Scala extends NamedToken
    case object Enable extends NamedToken
    case object Disable extends NamedToken
    case object Hide extends NamedToken
    case object As extends NamedToken
    case object Modifier extends NamedToken
    case object Synchronize extends NamedToken
    case object ReturnValue extends NamedToken

    case object And extends NamedToken("&")
    case object BracketLeft extends NamedToken("{")
    case object BracketRight extends NamedToken("}")
    case object SquareBracketLeft extends NamedToken("[")
    case object SquareBracketRight extends NamedToken("]")
    case object Arrow extends NamedToken("->")
    case object Comma extends NamedToken(",")
    case object ParenRight extends NamedToken(")")
    case object ParenLeft extends NamedToken("(")

    case class CodeBlock(sourceCode: String) extends NamedToken(s"$${${sourceCode.dropRight(1)}}")
    case class Identifier(str: String) extends NamedToken(s"Identifier($str)")
    case class Literal(str: String) extends NamedToken(s"Literal(\"$str\")")

}
