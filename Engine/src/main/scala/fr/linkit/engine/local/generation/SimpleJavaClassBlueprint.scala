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

package fr.linkit.engine.local.generation

import fr.linkit.api.local.generation.{JavaClassBlueprint, ValueScope}

import java.io.InputStream

class SimpleJavaClassBlueprint[V] private(blueprint: String, rootScope: ValueScope[V]) extends JavaClassBlueprint[V] {

    def this(blueprint: String, rootProvider: String => ValueScope[V]) = {
        this(blueprint, rootProvider(blueprint))
    }

    def this(stream: InputStream, rootProvider: String => ValueScope[V]) = {
        this(new String(stream.readAllBytes()), rootProvider)
    }

    override def getBlueprintString: String = blueprint

    override def toClassSource(v: V): String = rootScope.getSourceCode(v)
}