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

package fr.linkit.engine.gnom.referencing

import fr.linkit.api.gnom.referencing.NetworkObjectReference

import scala.language.existentials

object NOLUtils {

    @inline
    def trustedCast[T <: NetworkObjectReference](ref: X forSome {type X >: T}): T = {
        ref.asInstanceOf[T]
    }

    @inline
    def throwUnknownRef(ref: NetworkObjectReference): Nothing = {
        throw new UnsupportedOperationException(s"Unknown object reference '$ref'.")
    }

    @inline
    def throwUnknownObject(ref: AnyRef): Nothing = {
        throw new UnsupportedOperationException(s"Unknown object '$ref'.")
    }


}
