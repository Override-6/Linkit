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

package fr.linkit.api.local.resource.external

trait LocalExternalFolder extends LocalExternalResource with ResourceFolder {

    /**
     * Performs a non-recursive scan of all the content of this folder.
     * Each times the scan hits a resource that is not yet registered, the scanAction gets called.
     * scanAction may determine whether the hit resource must be registered or not, attached by
     * any representation kind, or destroyed...
     *
     * The implementation can perform default operations before or after invoking the scanAction.
     *
     * @param scanAction the action to perform on each new resource.
     * */
    def scan(scanAction: String => Unit): Unit

}
