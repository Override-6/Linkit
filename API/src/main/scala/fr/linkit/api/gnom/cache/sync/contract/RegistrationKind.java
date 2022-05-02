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

public enum RegistrationKind {

    /**
     * don't register the targeted object, ignore it.
     * */
    NotRegistered,
    /**
     * Register the object and make the object become a chipped object.
     * see {@link fr.linkit.api.gnom.cache.sync.ChippedObject}
     * */
    ChippedOnly,
    /**
     * Register and start synchronized object registration / generation process for the targeted object.
     * The object is also chipped, see {@link fr.linkit.api.gnom.cache.sync.SynchronizedObject}
     * */
    Synchronized,

    /**
     * Register a synchronized object that mirrors a distant Chipped Object / Synchronized Object.<br>
     * the object is a SynchronizedObject.
     * */
    Mirroring

}
