package fr.linkit.engine.internal.language.bhv.parse.line

import org.jetbrains.annotations.Nullable

trait Expression {

    val kind: String

    @Nullable val clojure: Clojure

}
