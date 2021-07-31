/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.cache.obj;

import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo;
import fr.linkit.api.connection.cache.obj.description.WrapperBehavior;

import java.io.Serializable;

public interface PuppetWrapper<T> extends Serializable {

    void initPuppeteer(Puppeteer<T> puppeteer);

    Puppeteer<T> getPuppeteer();

    WrapperBehavior<T> getBehavior();

    WrapperNodeInfo getWrapperNodeInfo();

    InvocationChoreographer getChoreographer();

    boolean isInitialized();

    T asWrapped();

    T detachedClone();

    Class<T> getWrappedClass();

}
