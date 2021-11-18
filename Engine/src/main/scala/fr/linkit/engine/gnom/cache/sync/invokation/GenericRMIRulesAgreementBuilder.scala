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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}

import scala.collection.mutable.ListBuffer

class GenericRMIRulesAgreementBuilder extends RMIRulesAgreementBuilder {

    private val currentIsOwner               = ownerID == currentID
    private val currentIsRootOwner           = rootOwnerID == currentID
    private val discarded                    = ListBuffer.empty[String]
    private val accepted                     = ListBuffer.empty[String]
    private var acceptAllTargets   : Boolean = true
    private var desiredEngineReturn: String  = currentID

    override def acceptRootOwner(): this.type = accept(rootOwnerID)

    override def discardRootOwner(): this.type = discard(rootOwnerID)

    override def desireCurrentEngineToReturn(): this.type = {
        desiredEngineReturn = currentID
        this
    }

    override def desireRootOwnerEngineToReturn(): this.type = {
        desiredEngineReturn = rootOwnerID
        this
    }

    override def ifCurrentIsRootOwner(action: RMIRulesAgreementBuilder => RMIRulesAgreementBuilder): this.type = {
        if (currentIsRootOwner) action(this)
        this
    }

    override def ifCurrentIsNotRootOwner(action: RMIRulesAgreementBuilder => RMIRulesAgreementBuilder): this.type = {
        if (!currentIsRootOwner) action(this)
        this
    }


    override def discard(target: String): this.type = {
        accepted -= target
        discarded += target
        this
    }

    override def accept(target: String): this.type = {
        discarded -= target
        accepted += target
        this
    }

    override def acceptOwner(): this.type = accept(ownerID)

    override def acceptAll(): this.type = {
        discarded.clear()
        accepted.clear()
        acceptAllTargets = true
        this
    }

    override def discardAll(): this.type = {
        discarded.clear()
        accepted.clear()
        acceptAllTargets = false
        this
    }

    override def acceptCurrent(): this.type = accept(currentID)

    override def discardCurrent(): this.type = discard(currentID)

    override def discardOwner(): this.type = discard(ownerID)

    override def setDesiredEngineReturn(target: String): this.type = {
        desiredEngineReturn = target
        this
    }

    override def desireOwnerEngineToReturn(): this.type = {
        setDesiredEngineReturn(ownerID)
    }

    override def ifCurrentIsOwner(action: RMIRulesAgreementBuilder => RMIRulesAgreementBuilder): this.type = {
        if (currentIsOwner) action(this)
        this
    }

    override def ifCurrentIsNotOwner(action: RMIRulesAgreementBuilder => RMIRulesAgreementBuilder): this.type = {
        if (!currentIsOwner) action(this)
        this
    }

    def result: RMIRulesAgreement = {
        new SimpleRMIRulesAgreement(currentID, ownerID, desiredEngineReturn, acceptAllTargets, accepted.toArray, discarded.toArray)
    }

}
