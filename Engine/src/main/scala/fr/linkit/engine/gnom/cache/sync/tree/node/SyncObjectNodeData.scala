package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.{SyncObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.tree.DefaultSynchronizedObjectTree
import org.jetbrains.annotations.Nullable

import java.lang.ref.WeakReference

class SyncObjectNodeData[A <: AnyRef](val puppeteer: Puppeteer[A],
                                      val synchronizedObject: A with SynchronizedObject[A],
                                      contract: StructureContract[A])
                                     (chip: Chip[A], //Reflective invocations
                                      @Nullable origin: WeakReference[AnyRef]) //The synchronized object's origin (the same object before it was converted to its synchronized version, if any).
                                     (reference: SyncObjectReference, //The sync object reference
                                      presence: NetworkObjectPresence, //the sync object presence
                                      tree: DefaultSynchronizedObjectTree[_], //The node's tree
                                      parent: Option[MutableSyncNode[_]])
    extends ChippedObjectNodeData[A](puppeteer.network, chip, contract, origin)(
        reference, presence, puppeteer.currentIdentifier, puppeteer.ownerID, tree, parent)