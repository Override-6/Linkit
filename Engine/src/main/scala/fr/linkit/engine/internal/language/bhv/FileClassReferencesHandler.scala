package fr.linkit.engine.internal.language.bhv

import fr.linkit.engine.internal.language.bhv.parse.{BhvFileReader, ParserAction}

import scala.collection.mutable

class FileClassReferencesHandler extends ParserAction {

    private val importedClasses = mutable.HashMap.empty[String, Class[_]]

    override def parse(reader: BhvFileReader): Unit = {

    }

    private def getClass(className: String): Class[_] = {
        importedClasses.getOrElseUpdate(className, Class.forName(className))
    }

}
