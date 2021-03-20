/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.system.fsa

import java.io.File
import scala.collection.mutable

abstract class AbstractFileSystemAdapter extends FileSystemAdapter {

    private val adapters = mutable.Map.empty[String, FileAdapter]

    override def getAdapter(path: String): FileAdapter = {
        val formatted = path
                .replace('\\', File.separatorChar)
                .replace('/', File.separatorChar)
        if (adapters.contains(formatted))
            return adapters(formatted)

        val adapter = createAdapter(formatted)
        adapters.put(formatted, adapter)
        adapter
    }

    protected def createAdapter(path: String): FileAdapter

}
