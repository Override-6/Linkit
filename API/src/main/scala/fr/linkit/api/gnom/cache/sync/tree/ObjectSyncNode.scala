package fr.linkit.api.gnom.cache.sync.tree

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.SynchronizedStructureContract
import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer

trait ObjectSyncNode[A <: AnyRef] extends SyncNode[A] {

    /**
     * The [[Puppeteer]] of the synchronized object.
     * @see [[Puppeteer]]
     */
    val puppeteer: Puppeteer[A]

    /**
     * The [[Chip]] of the synchronized object.
     * @see [[Chip]]
     */
    val chip: Chip[A]

    val contract : SynchronizedStructureContract[A]

    /**
     * The synchronized object.
     */
    val synchronizedObject: A with SynchronizedObject[A]


}
