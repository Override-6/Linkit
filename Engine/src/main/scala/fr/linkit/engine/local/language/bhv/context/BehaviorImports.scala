package fr.linkit.engine.local.language.bhv.context

import fr.linkit.api.local.language.bhv.context.ParsedBehavior

import scala.collection.mutable

class BehaviorImports extends ImportContext {

    private val importedClasses   = mutable.HashMap.empty[String, Class[_]]
    private val importedBehaviors = mutable.HashMap.empty[String, ParsedBehavior]

    override def addClassImport(clazz: Class[_]): Unit = {
        importedClasses.put(clazz.getSimpleName, clazz)
    }

    override def getClassImport(simpleName: String): Option[Class[_]] = {
        importedClasses.get(simpleName)
    }

    override def addBehaviorImport(behavior: ParsedBehavior): Unit = {
        importedBehaviors.put(behavior.getName, behavior)
    }

    override def getBehaviorImport(name: String): Option[ParsedBehavior] = {
        importedBehaviors.get(name)
    }

    override def mayConflict[T: {def getName: String}](t: T): Boolean = {
        val name = t.getName
        importedClasses.contains(name) || importedBehaviors.contains(name)
    }
}
