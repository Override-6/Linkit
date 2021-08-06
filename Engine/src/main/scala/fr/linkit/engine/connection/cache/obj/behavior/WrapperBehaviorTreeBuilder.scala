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

package fr.linkit.engine.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.{MemberBehaviorFactory, WrapperBehavior}

import scala.collection.mutable

abstract class WrapperBehaviorTreeBuilder(memberBehaviorFactory: MemberBehaviorFactory) {

    private val mappedBehaviors = mutable.HashMap.empty[Class[_], WrapperBehavior[_]]

    object behaviors {

        def +=(builder: WrapperBehaviorBuilder[_]): Unit = {
            mappedBehaviors.put(builder.classDesc.clazz, builder.build(memberBehaviorFactory))
        }
    }

    def build = new ObjectTreeDefaultBehavior(AnnotationBasedMemberBehaviorFactory, mappedBehaviors.toMap)

}
