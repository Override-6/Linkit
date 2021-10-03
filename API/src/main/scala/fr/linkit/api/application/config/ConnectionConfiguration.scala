/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.application.config

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.internal.system.security.BytesHasher

import java.net.URL

trait ConnectionConfiguration {

    val configName: String

    val identifier: String

    val hasher: BytesHasher

    val translatorFactory: ApplicationContext => ObjectTranslator

    val defaultPersistenceConfigScript: Option[URL]

}
