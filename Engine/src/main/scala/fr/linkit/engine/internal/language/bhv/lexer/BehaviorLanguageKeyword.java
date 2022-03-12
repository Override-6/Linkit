package fr.linkit.engine.internal.language.bhv.lexer;

public enum BehaviorLanguageKeyword implements BehaviorLanguageToken, Keyword {
    Import, Describe, Method, Scala, Enable, Stub,
    Mirroring, Disable, Statics, Hide, As, Modifier,
    Synchronize, ReturnValue;

    @Override
    public String representation() {
        return name().toLowerCase();
    }
}
