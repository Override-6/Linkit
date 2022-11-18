package fr.linkit.api.gnom.network.tag

import fr.linkit.api.gnom.network.Engine

trait EngineSelector {

    type NFETSelect = TagSelection[NetworkFriendlyEngineTag]

    implicit def retrieveNT(uniqueTag: UniqueTag with NetworkFriendlyEngineTag): NameTag

    def listNameTags(uniqueTag: NFETSelect): List[NameTag]

    def apply(tag: UniqueTag with NetworkFriendlyEngineTag): Engine = getEngine(tag).getOrElse {
        throw new NoSuchElementException(s"unable to find engine with tag '$tag' in this network.")
    }

    def getEngine(identifier: UniqueTag with NetworkFriendlyEngineTag): Option[Engine]

    /**
     * Returns true if tags a and b points to the same engine.
     * */
    def isEquivalent(a: NFETSelect, b: NFETSelect): Boolean = a == b || {
        val tagsA = listNameTags(a)
        val tagsB = listNameTags(b)
        tagsA.length == tagsB.length && tagsA.forall(tagsB.contains)
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


    implicit class NFETSelectOps(a: NFETSelect) {
        def <=>(b: NFETSelect): Boolean = isEquivalent(a, b)

        def C(b: NFETSelect): Boolean = isIncluded(a, b)
    }

    implicit class NFETOps(a: NetworkFriendlyEngineTag) {
        def <=>(b: NFETSelect): Boolean = isEquivalent(a, b)


        def C(b: NFETSelect): Boolean = isIncluded(a, b)
    }
}
