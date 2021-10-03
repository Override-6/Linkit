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

package fr.linkit.engine.gnom.persistence.context.profile.persistence;

import fr.linkit.api.gnom.persistence.context.TypePersistence;
import fr.linkit.api.gnom.persistence.obj.ObjectStructure;
import fr.linkit.engine.gnom.persistence.context.structure.ClassObjectStructure;

public class NativeTypePersistence<T> implements TypePersistence<T> {

    private final ObjectStructure structure;

    public NativeTypePersistence(Class<?> clazz) {
        this.structure = ClassObjectStructure.apply(clazz);
    }

    @Override
    public ObjectStructure structure() {
        return this.structure;
    }

    @Override
    public native void initInstance(T allocatedObject, Object[] args);

    @Override
    public native Object[] toArray(T t);
}
