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

import scala.collection.mutable.ListBuffer

class Channel(name: String) {
    
    private val messages = ListBuffer.empty[Message]
    
    def addMessage(message: String, owner: String): Message = {
        addMessage(new Message(message, messages.size, owner))
    }
    
    def getMessage(id: Int): Message = messages(id)
    
    def collectMessages(depth: Int): List[Message] = {
        if (depth < 0) return messages.toList
        messages.take(depth).toList
    }
    
    protected def addMessage(message: Message): Message = {
        messages += message
        message
    }
    
}
