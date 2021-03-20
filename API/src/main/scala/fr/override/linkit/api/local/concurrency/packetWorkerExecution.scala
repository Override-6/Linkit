/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.local.concurrency

import java.lang.annotation.{ElementType, Retention, RetentionPolicy, Target}
import scala.annotation.StaticAnnotation

/**
 * Specifies that this method or constructor must be executed by a packet worker thread
 * If the annotated code isn't running in the worker thread pool, some problem could occur. <br>
 *
 * @see [[IllegalThreadException]]
 * */
@Target(Array[ElementType](ElementType.CONSTRUCTOR, ElementType.METHOD))
@Retention(RetentionPolicy.CLASS)
class packetWorkerExecution extends StaticAnnotation {

}
