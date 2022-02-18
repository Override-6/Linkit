package fr.linkit.engine.internal.language.bhv.parsers

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.results.FileImports

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object ImportParser extends RegexParsers {


    private val importExp = rep("import ".r ~ ("[^\\s]+".r ^^ Class.forName))

    private def retrieveAllImports(reader: CharSequenceReader): Map[String, Class[_]] = {
        parse(importExp, reader) match {
            case Failure(msg, _)                        => throw new BHVLanguageException("Failure: " + msg)
            case Error(msg, _)                          => throw new BHVLanguageException("Error: " + msg)
            case Success(l: List[String ~ Class[_]], _) => l.map { case _ ~ c => (c.getSimpleName, c) }.toMap
        }
    }

    def retrieveImports(reader: CharSequenceReader): FileImports = {
        new FileImports(retrieveAllImports(reader))
    }
}