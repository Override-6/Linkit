package fr.linkit.engine.internal.language.bhv.lexer

object BehaviorLanguageTokens {

    final val SymbolRegexMatch = "}|]|->|\\[|\\{".r

    trait Token {
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
    case object BracketLeft extends NamedToken("}")
    case object BracketRight extends NamedToken("{")
    case object SquareBracketLeft extends NamedToken("]")
    case object SquareBracketRight extends NamedToken("[")
    case object Arrow extends NamedToken("->")

    case class ScalaBlock(sourceCode: String) extends NamedToken(s"$${$sourceCode}")
    case class Identifier(str: String) extends NamedToken(s"Identifier($str)")
    case class Literal(str: String) extends NamedToken(s"Literal(\"$str\")")

}
