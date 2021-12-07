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

package fr.linkit.api.gnom.cache.sync.contract.behavior

trait RMIRulesAgreementBuilder {

    type AgreementConditionAction = this.type => this.type

    def discard(target: String): this.type

    def accept(target: String): this.type

    def acceptAll(): this.type

    def discardAll(): this.type

    def acceptCurrent(): this.type

    def acceptOwner(): this.type

    def acceptCacheOwner(): this.type

    def acceptRootOwner(): this.type

    def discardCurrent(): this.type

    def discardOwner(): this.type

    def discardRootOwner(): this.type

    def discardCacheOwner(): this.type

    def setDesiredEngineReturn(target: String): this.type

    def desireCurrentEngineToReturn(): this.type

    def desireOwnerEngineToReturn(): this.type

    def desireRootOwnerEngineToReturn(): this.type

    def desireCacheOwnerEngineToReturn(): this.type

    def ifCurrentIsOwner(action: AgreementConditionAction): this.type

    def ifCurrentIsCacheOwner(action: AgreementConditionAction): this.type

    def ifCurrentIsRootOwner(action: AgreementConditionAction): this.type

    def ifCurrentIsNotOwner(action: AgreementConditionAction): this.type

    def ifCurrentIsNotCacheOwner(action: AgreementConditionAction): this.type

    def ifCurrentIsNotRootOwner(action: AgreementConditionAction): this.type

}

