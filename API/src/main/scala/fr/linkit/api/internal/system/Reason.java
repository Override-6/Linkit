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

package fr.linkit.api.internal.system;

//TODO Add more Detailed Reason, may have a Reason interface, and a Reason Enum
public enum Reason {
    INTERNAL(true, false),
    INTERNAL_ERROR(true, false),

    EXTERNAL_ERROR(false, true),
    EXTERNAL(false, true),

    SECURITY_CHECK(false, false),
    NOT_SPECIFIED(false, false);

    private final boolean isInternal;
    private final boolean isExternal;

    Reason(boolean isInternal, boolean isExternal) {
        this.isInternal = isInternal;
        this.isExternal = isExternal;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public boolean isExternal() {
        return isExternal;
    }


    public Reason reversedPOV() {
        switch (this) {
            case INTERNAL:
                return EXTERNAL;
            case INTERNAL_ERROR:
                return EXTERNAL_ERROR;

            case EXTERNAL:
                return INTERNAL;
            case EXTERNAL_ERROR:
                return INTERNAL_ERROR;

            default:
                return this;
        }
    }

}