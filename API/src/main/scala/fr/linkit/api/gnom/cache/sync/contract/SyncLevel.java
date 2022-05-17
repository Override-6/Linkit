/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.contract;

public enum SyncLevel {

    /**
     * don't register the targeted object, ignore it.
     */
    NotRegistered(false, false),
    /**
     * Register the object and make the object become a chipped object.
     * see {@link fr.linkit.api.gnom.cache.sync.ChippedObject}
     */
    ChippedOnly(true, true),
    /**
     * Register and start synchronized object registration / generation process for the targeted object.
     * The object is also chipped, see {@link fr.linkit.api.gnom.cache.sync.SynchronizedObject}
     */
    Synchronized(true, false),

    /**
     * Register a synchronized object that mirrors a distant Chipped Object / Synchronized Object.<br>
     * the object is a SynchronizedObject.
     */
    Mirroring(true, true),

    /**
     * Only registers the object, this way, the same instance of the object is sent <br>
     * to the remote engines instead of sending copy of it (see {@link fr.linkit.api.gnom.reference.NetworkObject})
     */
    Register(false, false),

    /**
     * Specific state for {@link fr.linkit.api.gnom.network.statics.StaticAccess} and Static accessors.
     */
    Statics(true, false),
    ;


    private final boolean isConnectable;
    private final boolean mustBeMirrored;

    SyncLevel(boolean isConnectable, boolean mustBeMirrored) {
        this.isConnectable = isConnectable;
        this.mustBeMirrored = mustBeMirrored;
    }

    public boolean isConnectable() {
        return isConnectable;
    }

    public boolean mustBeMirrored() {
        return mustBeMirrored;
    }
}
