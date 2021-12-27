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

package fr.linkit.api.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

public class HierarchyRaiserOrderer implements MethodOrderer {
    @Override
    public void orderMethods(MethodOrdererContext context) {
        context.getMethodDescriptors().sort((m1, m2) -> {
            var m1Class = m1.getMethod().getDeclaringClass();
            var m2Class = m2.getMethod().getDeclaringClass();
            return hierarchyLevel(m2Class) - hierarchyLevel(m1Class);
        });
    }

    private int hierarchyLevel(Class<?> clazz) {
        var superClass = clazz.getSuperclass();
        var level = 0;
        while (superClass != null) {
            superClass = superClass.getSuperclass();
            level++;
        }
        return level;
    }

}
