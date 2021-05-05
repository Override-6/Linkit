package fr.linkit.engine.local.generation.jcbp

import scala.collection.mutable.ListBuffer

object LexerUtils {

    def positions(str: String, blueprint: String): Seq[Int] = {
        var lastIndex = blueprint.indexOf(str)
        val locations = ListBuffer.empty[Int]
        while (lastIndex != -1) {
            locations += lastIndex
            lastIndex = blueprint.indexOf(str, lastIndex)
        }
        locations.toSeq
    }

}
