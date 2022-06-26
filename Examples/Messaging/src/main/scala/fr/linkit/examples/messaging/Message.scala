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

package fr.linkit.examples.messaging

import java.util.Date

class Message(var content: String,
              val id: Int,
              val ownerName: String) {
    
    private val creationDate = new Date
    private var lastModified = creationDate
    
    def modify(newMessage: String): Unit = {
        content = newMessage
        lastModified = new Date()
    }
    
    def getLastModified: Date = lastModified
    
}