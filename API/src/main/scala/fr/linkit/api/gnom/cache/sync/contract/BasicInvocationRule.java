
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

package fr.linkit.api.gnom.cache.sync.contract;

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreementBuilder;
import fr.linkit.api.gnom.cache.sync.contract.behavior.RemoteInvocationRule;

import static fr.linkit.api.gnom.cache.sync.contract.behavior.EngineTags.*;

/**
 * Basic RMI rules that defines simple agreements.
 */
public enum BasicInvocationRule implements RemoteInvocationRule {


    /**
     * Blocks every engines from remote invocation.<br>
     * The invocation will only be performed on the local object.<br>
     * This behavior is the same as calling any normal java method.<br>
     * <u>This is the default rule applied for methods</u>
     */
    ONLY_CURRENT((agreement) ->
        agreement.discardAll()
                .accept(CurrentEngine())
    ),
    /**
     * Invocation will only be performed on the engine that owns the original object.
     */
    ONLY_ORIGIN((agreement) ->
        agreement.discardAll()
                .accept(OwnerEngine())
                .appointReturn(OwnerEngine())
    ),
    /**
     * Invocation will only be performed on the engine that hosts the cache manager in which the object's
     * {@link fr.linkit.api.gnom.cache.sync.ConnectedObjectCache} is open.
     */
    ONLY_CACHE_OWNER((agreement) ->
        agreement.discardAll()
                .accept(CacheOwnerEngine())
                .appointReturn(CacheOwnerEngine())
    ),
    /**
     * The invocation will be performed on every remote machines, excluding the current machine.
     * The return value of the invocation will come from the machine that owns the original object.
     * However, If the current machine owns the object, the invocation will still be performed,
     * and the return value of the method will be taken from the local invocation result
     */
    NOT_CURRENT(((agreement) ->
        agreement.discard(CurrentEngine())
                .appointReturn(OwnerEngine())
    )),
    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines.
     * The return value of the invocation will come from the current machine.
     */
    BROADCAST(((agreement) ->
        agreement.acceptAll()
                .appointReturn(CurrentEngine())
    )),

    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines <b>only if</b> the current machine
     * is the owner of the object.
     * The return value of the invocation will come from the current machine.
     */
    BROADCAST_IF_ORIGINAL((agreement) ->
            agreement
                .assuming(CurrentEngine()).is(OwnerEngine(), RMIRulesAgreementBuilder::acceptAll)
                .accept(CurrentEngine())
                .appointReturn(CurrentEngine())
    ),


    /**
     * The invocation will be performed on the current machine <b>and</b> on every remote machines <b>only if</b> the current machine
     * is the owner of the root object.
     * The return value of the invocation will come from the current machine.
     */
    BROADCAST_IF_ROOT_OWNER((agreement ->
            agreement.assuming(CurrentEngine()).is(RootOwnerEngine(), RMIRulesAgreementBuilder::acceptAll)
                    .accept(CurrentEngine())
                    .appointReturn(CurrentEngine())
    )),
    /**
     * The invocation will be performed on the current machine <b>and</b> on the machine that owns the original object.
     * If the current machine owns the wrapper object, the execution will be called only once.
     * The return value of the invocation will come from the current machine.
     */
    CURRENT_AND_ORIGIN(((agreement) -> {
        return agreement.discardAll()
                .accept(CurrentEngine())
                .accept(OwnerEngine())
                .appointReturn(CurrentEngine());
    }));

    private final RemoteInvocationRule rule;

    BasicInvocationRule(RemoteInvocationRule rule) {
        this.rule = rule;
    }

    @Override
    public RMIRulesAgreementBuilder apply(RMIRulesAgreementBuilder agreement) {
        return rule.apply(agreement);
    }
}
