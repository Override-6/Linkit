/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.application.resource.external

import org.jetbrains.annotations.NotNull

/**
 * depicts a class that represents a resource file.
 * */
trait ResourceFile extends Resource {

    /**
     * @return The folder resource where this file is stored.
     *         The parent can't be null.
     * */
    @NotNull
    def getParent: ResourceFolder

    def getEntry: ResourceEntry[ResourceFile]

}
