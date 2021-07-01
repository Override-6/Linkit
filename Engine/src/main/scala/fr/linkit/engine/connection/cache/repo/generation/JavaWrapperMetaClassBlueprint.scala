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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.local.generation.cbp.AbstractClassBlueprint
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

class JavaWrapperMetaClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](getClass.getResourceAsStream("/generation/puppet_wrapper_meta_blueprint.jcbp")) {

    override val compilerType: CompilerType                         = CommonCompilerTypes.Javac
    override val rootScope   : RootValueScope = new RootValueScope {
        registerValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        registerValue("CompileTime" ~~> System.currentTimeMillis())
        registerValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        registerValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
    }
}
