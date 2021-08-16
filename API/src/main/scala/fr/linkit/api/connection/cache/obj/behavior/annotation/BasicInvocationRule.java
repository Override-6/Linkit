/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.cache.obj.behavior.annotation;

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreementBuilder;
import fr.linkit.api.connection.cache.obj.behavior.member.RemoteInvocationRule;

/**
 * Basic RMI rules that defines simple agreements.
 * */
public enum BasicInvocationRule implements RemoteInvocationRule {

    /**
     * Blocks every engines from remote invocation.
     * The invocation will only be performed on the local object.
     * This behavior is the same as calling any normal java method.
     */
    ONLY_CURRENT((agreement) -> {
        agreement.discardAll()
                .acceptCurrent();
    }),
    /**
     * Invocation will only be performed on the engine that owns the original object.
     */
    ONLY_OWNER((agreement) -> {
        agreement.discardAll()
                .acceptOwner()
                .setDesiredOwnerEngineReturn();
    }),
    /**
     * The invocation will be performed by every remote machines, excluding the current machine.
     * The return value of the invocation will come from the machine that owns the original object.
     * If the current machine owns the object, the invocation will still be performed, and the return value of the method
     * will be taken from the local invocation result
     */
    NOT_CURRENT(((agreement) -> {
        agreement.discardCurrent()
                .setDesiredOwnerEngineReturn();
    })),
    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines.
     * The return value of the invocation will come from the current machine.
     */
    BROADCAST(((agreement) -> {
        agreement.acceptAll()
                .setDesiredCurrentEngineReturn();
    })),

    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines <b>only if</b> the current machine
     * is the owner of the object.
     * The return value of the invocation will come from the current machine.
     */
    BROADCAST_IF_OWNER((agreement) -> {
        agreement
                .ifCurrentIsNotOwner(RMIRulesAgreementBuilder::discardAll)
                .setDesiredCurrentEngineReturn();
    }),


    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines <b>only if</b> the current machine
     * is the owner of the root object.
     * The return value of the invocation will come from the current machine.
     */
    //TODO Build the agreement
    BROADCAST_IF_ROOT_OWNER(BROADCAST),
    /**
     * The invocation will be performed on the current machine <b>and</b> on the machine that owns the original object.
     * If the current machine owns the wrapper object, the execution will be called only once.
     * The return value of the invocation will come from the current machine.
     */
    CURRENT_AND_OWNER(((agreement) -> {
        agreement.discardAll()
                .acceptCurrent()
                .acceptOwner()
                .setDesiredOwnerEngineReturn();
    }));

    private final RemoteInvocationRule rule;

    BasicInvocationRule(RemoteInvocationRule rule) {
        this.rule = rule;
    }

    @Override
    public void apply(RMIRulesAgreementBuilder agreement) {
        rule.apply(agreement);
    }
}
