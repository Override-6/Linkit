package fr.linkit.engine.internal.language.bhv

import fr.linkit.engine.internal.language.bhv.parse.{BhvFileReader, ParserAction}

class ImportedClasses private(classes: Map[String, Class[_]]) {

}

object ImportedClasses extends ParserAction[ImportedClasses] {
    override def parse(reader: BhvFileReader): ImportedClasses = {

    }
}
