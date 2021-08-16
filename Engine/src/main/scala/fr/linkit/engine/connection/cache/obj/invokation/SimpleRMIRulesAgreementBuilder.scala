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

package fr.linkit.engine.connection.cache.obj.invokation

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.engine.connection.cache.obj.invokation.SimpleRMIRulesAgreementBuilder.{CurrentID, OwnerID}

import scala.collection.mutable.ListBuffer

class SimpleRMIRulesAgreementBuilder(ownerID: String, currentID: String) extends RMIRulesAgreementBuilder {

    private val currentIsOwner               = ownerID == currentID
    private val discarded                    = ListBuffer.empty[String]
    private val accepted                     = ListBuffer.empty[String]
    private var acceptAllTargets   : Boolean = true
    private var desiredEngineReturn: String  = CurrentID

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

    override def acceptOwner(): this.type = accept(OwnerID)

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

    override def acceptCurrent(): this.type = accept(CurrentID)

    override def discardCurrent(): this.type = discard(CurrentID)

    override def discardOwner(): this.type = discard(OwnerID)

    override def setDesiredEngineReturn(target: String): this.type = {
        desiredEngineReturn = target
        this
    }

    override def setDesiredOwnerEngineReturn(): this.type = {
        setDesiredEngineReturn(OwnerID)
    }

    override def setDesiredCurrentEngineReturn(): this.type = {
        setDesiredEngineReturn(CurrentID)
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

object SimpleRMIRulesAgreementBuilder {
    //';' char is blacklisted so this string can be safely used as a flag
    val CurrentID: String = "a;Current"
    val OwnerID  : String = "a;Owner"

}
