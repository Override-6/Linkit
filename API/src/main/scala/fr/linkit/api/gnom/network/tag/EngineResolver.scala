package fr.linkit.api.gnom.network.tag

import fr.linkit.api.gnom.network.Engine

trait EngineResolver {

    def apply(tag: UniqueTag with NetworkFriendlyEngineTag): Engine = getEngine(tag).getOrElse {
        throw new NoSuchElementException(s"unable to find engine $tag in this network.")
    }

    def getEngine(identifier: UniqueTag with NetworkFriendlyEngineTag): Option[Engine]

    /**
     * Returns true if tags a and b points to the same engine.
     * */
    def isEquivalent(a: NetworkFriendlyEngineTag, b: NetworkFriendlyEngineTag): Boolean = {
        val es = listEngines(b)
        listEngines(a).forall(es.contains)
    }

    import TagUtils._
    isEquivalent(Nobody, !Everyone)

    /**
     * Verifies that all engines tagged by a are also tagged by b
     * */
    def isIncluded(a: NetworkFriendlyEngineTag, b: NetworkFriendlyEngineTag): Boolean

    /**
     * Searches all engines that are tagged by the given tag.
     * */
    def listEngines(tag: NetworkFriendlyEngineTag): List[Engine]

    /**
     * Verifies if it exists at least one engine that is tagged with the given tag.
     * */
    def exists(tag: NetworkFriendlyEngineTag): Boolean

}
