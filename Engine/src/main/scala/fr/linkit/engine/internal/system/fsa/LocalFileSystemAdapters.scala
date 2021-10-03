/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.system.fsa

import fr.linkit.engine.internal.system.fsa.io.IOFileSystemAdapter
import fr.linkit.engine.internal.system.fsa.nio.NIOFileSystemAdapter

object LocalFileSystemAdapters {

    lazy val Nio = new NIOFileSystemAdapter
    lazy val Io  = new IOFileSystemAdapter

}
