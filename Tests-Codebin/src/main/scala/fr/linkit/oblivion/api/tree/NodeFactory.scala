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

package fr.linkit.oblivion.api.tree

trait NodeFactory[T] {

    def canHandle(clazz: Class[_]): Boolean

    def canHandle(info: ByteSeq): Boolean

    def newNode(finder: NodeFinder, profile: ClassProfile[T]): SerialNode[T]

    def newNode(finder: NodeFinder, bytes: ByteSeq): DeserialNode[T]

}
