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

package fr.linkit.api.gnom.persistence.context

/**
 * The Control box is used during object deserialisation.
 * The control box is currently very simple but could get more complex in the future
 * Used to handle async object deserialisation
 * */
trait ControlBox {


    /**
     * informs the control box that an async task will be performed.
     * */
    def beginTask(): Unit

    /**
     * Informs the control box that an async tas has ended.
     * */
    def releaseTask(): Unit

    /**
     * The thread that executes the method will wait until all signaled async tasks are terminated.
     * */
    def join(): Unit

}
