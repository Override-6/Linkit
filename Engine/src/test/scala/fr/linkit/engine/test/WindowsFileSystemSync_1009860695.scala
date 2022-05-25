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

package fr.linkit.engine.test

import sun.nio.fs.WindowsFileSystem
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.engine.gnom.cache.sync.AbstractSynchronizedObject
import fr.linkit.engine.internal.utils.JavaUtils.nl

/*
* this class defines the main fields
* and methods required for a SynchronizedObject
* */
@SerialVersionUID(1653476642410L)
class WindowsFileSystemSync_1009860695 extends AbstractSynchronizedObject[WindowsFileSystem] {

    override def originClass: Class[_] = classOf[WindowsFileSystem]


    private implicit def cast_1[_1, X[_]](y: X[_]): X[_1] = y.asInstanceOf[X[_1]]



     def getUserPrincipalLookupService(): java.nio.file.attribute.UserPrincipalLookupService = {
        handleCall[java.nio.file.attribute.UserPrincipalLookupService](501216677)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.nio.file.attribute.UserPrincipalLookupService]
    }

    override def toString(): java.lang.String = {
        handleCall[java.lang.String](-581662510)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.lang.String]
    }

    override def clone(): java.lang.Object = {
        handleCall[java.lang.Object](1158633201)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.lang.Object]
    }

     def newWatchService(): java.nio.file.WatchService = {
        handleCall[java.nio.file.WatchService](-1874058567)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.nio.file.WatchService]
    }

    override def equals(arg1: scala.Any): Boolean = {
        handleCall[Boolean](-166894183)(Array(arg1))(args => Array(this: Any , arg1): Any).asInstanceOf[Boolean]
    }

     def getPathMatcher(arg1: java.lang.String): java.nio.file.PathMatcher = {
        handleCall[java.nio.file.PathMatcher](-1610380484)(Array(arg1))(args => Array(this: Any , arg1): Any).asInstanceOf[java.nio.file.PathMatcher]
    }

     def isOpen(): Boolean = {
        handleCall[Boolean](-1115525443)(Array())(args => Array(this: Any ): Any).asInstanceOf[Boolean]
    }

     def provider(): java.nio.file.spi.FileSystemProvider = {
        handleCall[java.nio.file.spi.FileSystemProvider](1273646972)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.nio.file.spi.FileSystemProvider]
    }

     def close(): Unit = {
        handleCall[Unit](98381709)(Array())(args => Array(this: Any ): Any).asInstanceOf[Unit]
    }

    override def finalize(): Unit = {
        handleCall[Unit](-677630541)(Array())(args => Array(this: Any ): Any).asInstanceOf[Unit]
    }

     def getSeparator(): java.lang.String = {
        handleCall[java.lang.String](-1376482667)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.lang.String]
    }

     def isReadOnly(): Boolean = {
        handleCall[Boolean](-1596242475)(Array())(args => Array(this: Any ): Any).asInstanceOf[Boolean]
    }

     def getRootDirectories(): java.lang.Iterable[Nothing] = {
        handleCall[java.lang.Iterable[Nothing]](219436314)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.lang.Iterable[Nothing]]
    }

     def supportedFileAttributeViews(): java.util.Set[Nothing] = {
        handleCall[java.util.Set[Nothing]](-1637608001)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.util.Set[Nothing]]
    }

     def getFileStores(): java.lang.Iterable[Nothing] = {
        handleCall[java.lang.Iterable[Nothing]](-1515518453)(Array())(args => Array(this: Any ): Any).asInstanceOf[java.lang.Iterable[Nothing]]
    }

    override def hashCode(): Int = {
        handleCall[Int](147801099)(Array())(args => Array(this: Any ): Any).asInstanceOf[Int]
    }


}
