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

package fr.linkit.api.gnom.persistence.obj

trait ObjectStructure {

    def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean

    def isAssignable(args: Array[Class[_]]): Boolean = isAssignable(args, 0, args.length)

    def isAssignable(args: Array[Any]): Boolean = isAssignable(args, 0, args.length)

    def isAssignable(args: Array[Any], from: Int, to: Int): Boolean


}
