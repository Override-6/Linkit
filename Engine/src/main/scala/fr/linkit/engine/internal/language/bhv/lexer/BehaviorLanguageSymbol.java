package fr.linkit.engine.internal.language.bhv.lexer;

import scala.util.matching.Regex;

public enum BehaviorLanguageSymbol implements BehaviorLanguageToken, Symbol  {
    Not("!"),
    And("&"),
    Equals("="),
    BracketLeft("{"),
    BracketRight("}"),
    SquareBracketLeft("["),
    SquareBracketRight("]"),
    Arrow("->"),
    Comma(","),
    ParenRight(")"),
    ParenLeft("(");

    private final String rep;

    public static final Regex RegexSymbols = Symbol.makeRegex(values());

    BehaviorLanguageSymbol(String rep) {
        this.rep = rep;
    }

    @Override
    public String representation() {
        return rep;
    }
}
