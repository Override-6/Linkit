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

package fr.linkit.api.gnom.cache.sync.invokation.local
import fr.linkit.api.gnom.cache.sync.contract.behavior.member.method.InternalMethodBehavior

trait CallableLocalMethodInvocation[R] extends LocalMethodInvocation[R] {

    override val methodBehavior: InternalMethodBehavior

    /**
     * Calls the local method of the object.
     * @return the method's call result.
     * */
    def callSuper(): R

}
