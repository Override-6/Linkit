package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.behavior.RMIRulesAgreement

class SimpleRMIRulesAgreement(currentID: String, ownerID: String,
                              desiredEngineReturn: String, acceptAll: Boolean,
                              accepted: Array[String], excluded: Array[String]) extends RMIRulesAgreement {
    private val currentIsOwner = currentID == ownerID

    override val acceptedEngines: Array[String] = accepted

    override val discardedEngines: Array[String] = excluded

    override def isAcceptAll: Boolean = acceptAll

    override def getDesiredEngineReturn: String = desiredEngineReturn

    override def mayCallSuper: Boolean = {
        if (acceptAll)
            !(excluded.contains(currentID) && (currentIsOwner || excluded.contains(ownerID)))
        else
            accepted.contains(currentID) && (currentIsOwner || accepted.contains(ownerID))
    }

    override val mayPerformRemoteInvocation: Boolean = {
        acceptAll || (accepted.nonEmpty && !accepted.contains(currentID))
    }

}
