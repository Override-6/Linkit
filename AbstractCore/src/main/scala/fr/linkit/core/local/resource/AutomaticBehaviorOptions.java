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

package fr.linkit.core.local.resource;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static java.nio.file.StandardWatchEventKinds.*;

public enum AutomaticBehaviorOptions {

    AUTO_REGISTER(ENTRY_CREATE),
    AUTO_UNREGISTER(ENTRY_DELETE),
    AUTO_UPDATE_CHECKSUM(ENTRY_MODIFY);

    private final WatchEvent.Kind<Path> watchEventKind;

    AutomaticBehaviorOptions(WatchEvent.Kind<Path> watchEventKind) {
        this.watchEventKind = watchEventKind;
    }

    public WatchEvent.Kind<Path> getWatchEventKind() {
        return watchEventKind;
    }
}
