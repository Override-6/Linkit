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

package fr.linkit.api.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller

trait StaticsCaller extends MethodCaller {

    val staticsTarget: Class[_]

}

object StaticsCaller {
    def getStaticsTarget(clazz: Class[_ <: StaticsCaller]): Class[_] = {
        val companionClass = clazz.getClassLoader.loadClass(clazz.getName + "$")
        val companionField = companionClass.getDeclaredField("MODULE$")
        companionField.setAccessible(true)
        val companion = companionField.get(null)
        val staticsTargetMethod = companionClass.getDeclaredMethod("staticsTarget")
        staticsTargetMethod.setAccessible(true)
        val staticsTarget = staticsTargetMethod.invoke(companion)
        staticsTarget.asInstanceOf[Class[_]]
    }
}
