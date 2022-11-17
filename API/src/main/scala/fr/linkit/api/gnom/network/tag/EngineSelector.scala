package fr.linkit.api.gnom.network.tag

import fr.linkit.api.gnom.cache.sync.contract.OwnerEngine
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.network.tag.TagSelection.TagOps

trait EngineSelector {

    type NFETSelect = TagSelection[NetworkFriendlyEngineTag]

    def apply(tag: UniqueTag with NetworkFriendlyEngineTag): Engine = getEngine(tag).getOrElse {
        throw new NoSuchElementException(s"unable to find engine $tag in this network.")
    }

    def getEngine(identifier: UniqueTag with NetworkFriendlyEngineTag): Option[Engine]

    /**
     * Returns true if tags a and b points to the same engine.
     * */
    def isEquivalent(a: NFETSelect, b: NFETSelect): Boolean = {
        val es = listEngines(b)
        listEngines(a).forall(es.contains)
    }

    /**
     * Verifies that all engines tagged by a are also tagged by b
     * */
    def isIncluded(a: NFETSelect, b: NFETSelect): Boolean

    /**
     * Searches all engines that are tagged by the given tag.
     * */
    def listEngines(tag: NFETSelect): List[Engine]

    /**
     * Verifies if it exists at least one engine that is tagged with the given tag.
     * */
    def exists(tag: NFETSelect): Boolean

}
