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

package fr.linkit.engine.gnom.cache.obj.tree.node

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{SyncNodeLocation, SynchronizedObjectTree}

class ObjectNodeData[A <: AnyRef](val puppeteer: Puppeteer[A], //Remote invocation
                                  val chip: Chip[A], //Reflective invocation
                                  val tree: SynchronizedObjectTree[_],
                                  val location: SyncNodeLocation,
                                  val synchronizedObject: A with SynchronizedObject[A],
                                  val currentIdentifier: String) {

}
