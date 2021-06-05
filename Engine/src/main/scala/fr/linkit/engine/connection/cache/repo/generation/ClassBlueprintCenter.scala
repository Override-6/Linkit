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

import fr.linkit.api.local.generation.cbp.ClassBlueprint
import fr.linkit.engine.connection.cache.repo.generation.ClassBlueprintCenter.{JCBPPath, SCBPPath}
import fr.linkit.engine.connection.cache.repo.generation.PuppetWrapperClassGenerator.ClassValueScope
import fr.linkit.engine.local.generation.cbp.SimpleClassBlueprint

import scala.collection.mutable

class ClassBlueprintCenter[T] {

    private val defaultJcbp = new SimpleClassBlueprint(getClass.getResourceAsStream(JCBPPath), new ClassValueScope(_))
    private val defaultScbp = new SimpleClassBlueprint(getClass.getResourceAsStream(SCBPPath), new ClassValueScope(_))

    private val otherCbps = mutable.Map.empty[Class[_], ClassBlueprint[T]]

    /*def getBlueprint(clazz: Class[_]): ClassBlueprint[T] = otherCbps.getOrElse(clazz, {
        defaultScbp
    })*/

}

object ClassBlueprintCenter {

    val JCBPPath: String = "/generation/puppet_wrapper_blueprint.jcbp"
    val SCBPPath: String = "/generation/puppet_wrapper_blueprint.scbp"

}
