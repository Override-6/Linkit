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

package fr.linkit.engine.gnom.cache.sync.generation.reference

import fr.linkit.api.internal.generation.compilation.CompilationContext
import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncObjectClassResource._

class ExtendedReferenceCompilationContext(val clazz: Class[_]) extends CompilationContext {

    override def classPackage: String = WrapperPackage + clazz.getPackageName

    override def className: String = clazz.getSimpleName + "Extended"

    override def parentLoader: ClassLoader = clazz.getClassLoader
}
