package fr.linkit.engine.internal.language.bhv.lexer;

import scala.util.matching.Regex;

public enum ScalaCodeBlockSymbol implements ScalaCodeBlockToken, Symbol {

    ValueOpen("£{"), ValueClose("}"), Colon(":");

    private final String rep;

    public static final Regex RegexSymbols = Symbol.makeRegex(values());

    ScalaCodeBlockSymbol(String rep) {
        this.rep = rep;
    }

    @Override
    public String representation() {
        return rep;
    }

}
