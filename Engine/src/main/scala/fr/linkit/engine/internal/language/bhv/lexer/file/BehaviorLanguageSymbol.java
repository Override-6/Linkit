/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.lexer.file;

import fr.linkit.engine.internal.language.bhv.lexer.Symbol;
import scala.util.matching.Regex;

public enum BehaviorLanguageSymbol implements BehaviorLanguageToken, Symbol {
    Exclamation("!"),

    Equal("="),
    Arrow("->"),

    FatArrow("=>"),
    Comma(","),
    Dot("."),
    Colon(":"),
    Star("*"),
    At("@"),
    BracketLeft("{"),
    BracketRight("}"),
    SquareBracketLeft("["),
    SquareBracketRight("]"),
    ParenRight(")"),
    ParenLeft("(");

    private final String rep;

    public static final Regex RegexSymbols = Symbol.makeRegex(values());

    BehaviorLanguageSymbol(String symbol) {
        this.rep = symbol;
    }

    @Override
    public String value() {
        return rep;
    }

    @Override
    public String toString() {
        return "symbol '" + value() + "'";
    }
}
