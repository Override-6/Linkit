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

package fr.linkit.api.local.resource.representation

import org.jetbrains.annotations.NotNull

/**
 * depicts a class that represents a resource file.
 * */
trait ResourceFile extends ResourceRepresentation {

    /**
     * @return The folder resource where this file is stored.
     *         The parent can't be null.
     * */
    @NotNull
    def getParent: ResourceFolder

}
