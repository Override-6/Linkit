/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.persistence.context

import fr.linkit.api.gnom.referencing.linker.ContextObjectLinker
import fr.linkit.api.gnom.referencing.traffic.TrafficInterestedNPH

trait PersistenceConfig extends TypeProfileStore {
    
    def contextualObjectLinker: ContextObjectLinker with TrafficInterestedNPH

}
