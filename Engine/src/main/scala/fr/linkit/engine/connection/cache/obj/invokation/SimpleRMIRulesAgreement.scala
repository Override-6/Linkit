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

import scala.collection.mutable.ListBuffer

class SimpleRMIRulesAgreement(currentID: String, ownerID: String) extends RMIRulesAgreementBuilder with RMIRulesAgreement {

    private val discarded                    = ListBuffer.empty[String]
    private val accepted                     = ListBuffer.empty[String]
    private var acceptAllTargets   : Boolean = true
    private var desiredEngineReturn: String  = currentID

    private val isCurrentOwner = currentID == ownerID

    override def discard(target: String): SimpleRMIRulesAgreement.this.type = {
        accepted -= target
        discarded += target
        this
    }

    override def accept(target: String): SimpleRMIRulesAgreement.this.type = {
        discarded -= target
        accepted += target
        this
    }

    override def acceptOwner(): SimpleRMIRulesAgreement.this.type = accept(ownerID)

    override def acceptAll(): SimpleRMIRulesAgreement.this.type = {
        discarded.clear()
        accepted.clear()
        acceptAllTargets = true
        this
    }

    override def discardAll(): SimpleRMIRulesAgreement.this.type = {
        discarded.clear()
        accepted.clear()
        acceptAllTargets = false
        this
    }

    override def acceptCurrent(): SimpleRMIRulesAgreement.this.type = accept(currentID)

    override def discardCurrent(): SimpleRMIRulesAgreement.this.type = discard(currentID)

    override def discardOwner(): SimpleRMIRulesAgreement.this.type = discard(ownerID)

    override def setDesiredEngineReturn(target: String): SimpleRMIRulesAgreement.this.type = {
        desiredEngineReturn = target
        this
    }

    override def getAcceptedEngines: Array[String] = accepted.toArray

    override def getDiscardedEngines: Array[String] = discarded.toArray

    override def isAcceptAll: Boolean = acceptAllTargets

    override def getDesiredEngineReturn: String = desiredEngineReturn

    override def mayCallSuper: Boolean = {
        if (acceptAllTargets)
            !(discarded.contains(currentID) && (isCurrentOwner || discarded.contains(ownerID)))
        else
            accepted.contains(currentID) && (isCurrentOwner || accepted.contains(ownerID))
    }
}
