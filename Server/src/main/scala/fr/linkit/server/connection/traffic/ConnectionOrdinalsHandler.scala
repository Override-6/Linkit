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

package fr.linkit.server.connection.traffic

import fr.linkit.server.connection.traffic.ConnectionOrdinalsHandler.OrdinalCounter

import scala.collection.mutable

class ConnectionOrdinalsHandler {
    
    private final val ordinal = mutable.HashMap.empty[Int, OrdinalCounter]
    
    def forChannel(path: Array[Int]): OrdinalCounter = {
        ordinal.getOrElseUpdate(java.util.Arrays.hashCode(path), new OrdinalCounter)
    }
    
}

object ConnectionOrdinalsHandler {
    
    class OrdinalCounter() {
        
        private var count = 0
        
        def next(): Int = {
            count += 1
            count
        }
    }
}
