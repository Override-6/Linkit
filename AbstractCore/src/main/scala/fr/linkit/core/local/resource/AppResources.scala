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

package fr.linkit.core.local.resource

import fr.linkit.api.local.resource.{ExternalResourceFolder, ExternalResourceFile}
import fr.linkit.api.local.system.fsa.FileSystemAdapter

import java.io.InputStream

class AppResources(fsa: FileSystemAdapter, absoluteLocation: String) extends ExternalResourceFolder {

    override def getLocation: String = absoluteLocation

    override def getResource(name: String): ExternalResourceFile = ???

    override def getResourceAsStream(name: String): InputStream = ???

    override def getResources(name: String): ExternalResourceFolder = ???
}
