package fr.linkit.engine.connection.cache.obj.invokation

import fr.linkit.api.connection.cache.obj.behavior.RMIRulesAgreement

class SimpleRMIRulesAgreement(currentID: String, ownerID: String,
                              desiredEngineReturn: String, acceptAll: Boolean,
                              accepted: Array[String], excluded: Array[String]) extends RMIRulesAgreement {
    private val currentIsOwner = currentID == ownerID

    override def getAcceptedEngines: Array[String] = accepted

    override def getDiscardedEngines: Array[String] = excluded

    override def isAcceptAll: Boolean = acceptAll

    override def getDesiredEngineReturn: String = desiredEngineReturn

    override def mayCallSuper: Boolean = {
        if (acceptAll)
            !(excluded.contains(currentID) && (currentIsOwner || excluded.contains(ownerID)))
        else
            accepted.contains(currentID) && (currentIsOwner || accepted.contains(ownerID))
    }

    override def mayPerformRemoteInvocation: Boolean = {
        acceptAll || (accepted.nonEmpty && !accepted.contains(currentID))
    }

}
