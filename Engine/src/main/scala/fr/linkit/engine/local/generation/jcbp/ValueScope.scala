package fr.linkit.engine.local.generation.jcbp

import fr.linkit.api.local.generation.{ValueInserter, ValueProvider}
import org.jetbrains.annotations.Nullable

class ValueScope(name: String,
                 blueprint: String,
                 provider: ValueProvider,
                 @Nullable upperScope: ValueScope) {

    private val blueprints = {
        LexerUtils
            .positions("$$" + name + "$$", blueprint)
            .map(getSubBlueprint)
    }

    def insertAllValues(inserter: ValueInserter): Unit = {

    }

    private def getSubBlueprint(pos: Int): String = {
        val bracketStart   = blueprint.indexOf('{', pos)
        var blockDepth     = 0
        val semiBlock      = blueprint.drop(bracketStart)
        var lastChar: Char = null
        var isInString     = true
        for (i <- semiBlock.indices) {
            semiBlock(i) match {
                case '\"' if lastChar != '\\'              => isInString = !isInString
                case '}' if blockDepth == 0 && !isInString => return semiBlock.take(i)
                case '{' if !isInString                    => blockDepth += 1
                case '}' if !isInString                    => blockDepth -= 1
                case _                                     =>
            }
            lastChar = semiBlock(i)
        }
        semiBlock
    }
}