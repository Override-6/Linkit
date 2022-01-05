package fr.linkit.engine.internal.language.bhv.parsers

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.results.FileImports

import java.io.{Reader, StringReader}
import scala.collection.mutable
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object ImportParser extends RegexParsers {
    private val importExp = "import ".r ~ ("[^\\s]+".r ^^ Class.forName)

    def retrieveImports(file: String): FileImports = {
        new FileImports(retrieveAllImports(file))
    }

    private def retrieveAllImports(file: String): Map[String, Class[_]] = {
        var next: Input = new CharSequenceReader(file)
        val map         = mutable.HashMap.empty[String, Class[_]]
        while (!next.atEnd) {
            parseAll(importExp, file) match {
                  case Failure(msg, _) => throw new BHVLanguageException("Failure: " + msg)
                case Error(msg, _)   => throw new BHVLanguageException("Error: " + msg)
                case Success(_ ~ cl, n)  =>
                    next = n
                    map put (cl.getSimpleName, cl)
            }
        }
        map.toMap
    }
}