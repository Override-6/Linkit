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

package fr.linkit.api.application.resource.external

trait LocalFolder extends LocalResource with ResourceFolder {

    /**
     * Performs a non-recursive scan of all the content of this folder, excluding folders.
     * Each times the scan hits a resource that is not yet registered, the scanAction gets called.
     * scanAction may determine whether the hit resource must be registered or not, attached by
     * any representation kind, or destroyed...
     *
     * The implementation can perform default operations before or after invoking the scanAction.
     *
     * @param scanAction the action to perform on each new resource.
     * */
    def scanFiles(scanAction: String => Unit): Unit


    /**
     * Performs a non-recursive scan of all the content of this folder, excluding files.
     * Each times the scan hits a resource that is not yet registered, the scanAction gets called.
     * scanAction may determine whether the hit resource must be registered or not, attached by
     * any representation kind, or destroyed...
     *
     * The implementation can perform default operations before or after invoking the scanAction.
     *
     * @param scanAction the action to perform on each new resource.
     * */
    def scanFolders(scanAction: String => Unit): Unit

}
