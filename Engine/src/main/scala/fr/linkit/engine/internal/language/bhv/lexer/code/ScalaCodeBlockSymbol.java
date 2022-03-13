/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv.lexer.code;

import fr.linkit.engine.internal.language.bhv.lexer.Symbol;
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
