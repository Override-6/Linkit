package fr.linkit.api.gnom.cache.sync.contract.level;

public enum MirrorableSyncLevel implements SyncLevel {

    /**
     * Register the object and make the object become a chipped object.
     * see {@link fr.linkit.api.gnom.cache.sync.ChippedObject}
     */
    Chipped,
    /**
     * Register a synchronized object that mirrors a distant Chipped Object / Synchronized Object.<br>
     * the object is a SynchronizedObject.
     */
    Mirror

}
