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

package fr.linkit.api.application.config

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.internal.system.security.BytesHasher

import java.net.URL

trait ConnectionConfiguration {

    val configName: String

    val connectionName: String

    val hasher: BytesHasher

    val translatorFactory: ApplicationContext => ObjectTranslator

    val defaultPersistenceConfigScript: Option[URL]

}
