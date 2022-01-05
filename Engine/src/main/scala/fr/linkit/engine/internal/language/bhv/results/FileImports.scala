package fr.linkit.engine.internal.language.bhv.results

import scala.util.Try

class FileImports(imports: Map[String, Class[_]]) {
    def find(name: String): Option[Class[_]] = {
        imports.get(name).orElse(Try(Class.forName(name)).toOption)
    }
}
