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

package fr.linkit.api.gnom.cache.sync.generation

import java.net.URLClassLoader
import java.nio.file.Path

/**
 * The class loader that loads the generated [[fr.linkit.api.gnom.cache.sync.SynchronizedObject]] classes.
 * @param classRootFolder the root folder of the class (the folder that stores the first package of the class)
 * @param parent the parent class loader
 * @param mates
 */
class GeneratedClassLoader(val classRootFolder: Path, parent: ClassLoader, mates: Seq[ClassLoader]) extends URLClassLoader(Array(classRootFolder.toUri.toURL), parent) {

    override def loadClass(name: String, resolve: Boolean): Class[_] = {
        try {
            super.loadClass(name, resolve)
        } catch {
            case _: ClassNotFoundException => {
                var clazz: Class[_] = null
                var i               = 0
                while ((clazz == null) && i < mates.length) {
                    try {
                        clazz = mates(i).loadClass(name)
                    } catch {
                        case e: ClassNotFoundException =>
                            if (i >= mates.length)
                                throw e
                    }
                    i += 1
                }
                if (clazz == null)
                    throw new ClassNotFoundException(name)
                clazz
            }
        }
    }

    def defineClass(bytecode: Array[Byte], className: String): Class[_] = {
        super.defineClass(className, bytecode, 0, bytecode.length)
    }
}
