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

package fr.linkit.engine.internal.language.bhv.lexer.file;

import fr.linkit.engine.internal.language.bhv.lexer.Keyword;

public enum BehaviorLanguageKeyword implements BehaviorLanguageToken, Keyword {
    Import, Describe, Method, Scala, Enable, Stub,
    Mirroring, Disable, Statics, Hide, As, Modifier,
    Sync, ReturnValue, Foreach,
    //Agreements
    Agreement,
    Discard, Accept, And, Appoint, If, Else, Is, Not
    ;

    @Override
    public String representation() {
        return name().toLowerCase();
    }
}
