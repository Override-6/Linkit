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

package fr.linkit.api.gnom.cache.sync.contract.level;

public enum ConcreteSyncLevel implements SyncLevel {

    /**
     * don't register the targeted object, ignore it.
     */
    NotRegistered,

    /**
     * Register and start synchronized object registration / generation process for the targeted object.
     * The object is also chipped, see {@link fr.linkit.api.gnom.cache.sync.SynchronizedObject}
     */
    Synchronized,


    /**
     * Specific state for {@link fr.linkit.api.gnom.network.statics.StaticAccess} and Static accessors.
     */
    Statics,


}
