package fr.linkit.engine.local.generation.jcbp

import fr.linkit.api.local.generation.{BlueprintValue, ValueInserter}

class AbstractBlueprintValue[A](name: String, blueprint: String) extends BlueprintValue[A] {

    private val positions = LexerUtils.positions('$' + name + '$', blueprint)

    override def replaceValues(inserter: ValueInserter, value: A): Unit = {
        val str = toClauseString(value)
        positions.foreach(inserter.insert(str, name, _))
    }

    def toClauseString(a: A): String

}
