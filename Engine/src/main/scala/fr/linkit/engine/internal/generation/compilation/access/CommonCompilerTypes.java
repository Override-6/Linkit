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

package fr.linkit.engine.internal.generation.compilation.access;

import fr.linkit.api.internal.generation.compilation.access.CompilerType;

public enum CommonCompilerTypes implements CompilerType {

    //TODO add Kotlin compiler support.
    Javac("Java", ".java"), Scalac("Scala", ".scala");

    private final String langName;
    private final String extension;

    CommonCompilerTypes(String langName, String extension) {
        this.langName = langName;
        this.extension = extension;
    }

    @Override
    public String languageName() {
        return langName;
    }

    @Override
    public String sourceFileExtension() {
        return extension;
    }
}
