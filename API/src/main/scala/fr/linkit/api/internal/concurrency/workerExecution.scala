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

package fr.linkit.api.internal.concurrency

import java.lang.annotation.{ElementType, Retention, RetentionPolicy, Target}
import scala.annotation.StaticAnnotation

/**
 * Specifies that this method or constructor must be executed by a worker thread
 * If the annotated code isn't running in a worker thread pool, some problem could occur. <br>
 *
 * @see [[BusyWorkerPool]]
 * @see [[IllegalThreadException]]
 * */
@Target(Array[ElementType](ElementType.CONSTRUCTOR, ElementType.METHOD))
@Retention(RetentionPolicy.CLASS)
class workerExecution extends StaticAnnotation {

}
