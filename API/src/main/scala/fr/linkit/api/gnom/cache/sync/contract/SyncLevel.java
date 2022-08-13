/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
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
    NotRegistered(false, false, false),
    /**
     * Register the object and make the object become a chipped object.
     * see {@link fr.linkit.api.gnom.cache.sync.ChippedObject}
     */
    Chipped(true, true, true),
    /**
     * Register and start synchronized object registration / generation process for the targeted object.
     * The object is also chipped, see {@link fr.linkit.api.gnom.cache.sync.SynchronizedObject}
     */
    Synchronized(true, true,false),

    /**
     * Register a synchronized object that mirrors a distant Chipped Object / Synchronized Object.<br>
     * the object is a SynchronizedObject.
     */
    Mirror(true, true,true),

    /**
     * Only registers the object, this way, the same instance of the object is sent <br>
     * to the remote engines instead of sending copy of it (see {@link fr.linkit.api.gnom.referencing.NetworkObject})
     */
    Register(false, false,false),//Note: Currently Not Used

    /**
     * Specific state for {@link fr.linkit.api.gnom.network.statics.StaticAccess} and Static accessors.
     */
    Statics(false, true,false),
    ;

    /**
     * true if the syncLevel represents an object that can be connected and used by other engines on the network.
     * */
    private final boolean isConnectable;
    /**
     * true if the syncLevel must be set to Mirror if it is sent to an engine that is not the original owner of the object.
     * */
    private final boolean mustBeMirrored;
    /**
     * true if the syncLevel implies a contract.
     * */
    private final boolean isContractable;

    SyncLevel(boolean isConnectable, boolean isContractable, boolean mustBeMirrored) {
        this.isConnectable = isConnectable;
        this.mustBeMirrored = mustBeMirrored;
        this.isContractable = isContractable;
    }

    public boolean isConnectable() {
        return isConnectable;
    }

    public boolean isContractable() {
        return isContractable;
    }

    public boolean mustBeMirrored() {
        return mustBeMirrored;
    }
}
