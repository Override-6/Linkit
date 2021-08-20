package fr.linkit.engine.local.language.bhv.context

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, SystemBehavior}
import fr.linkit.api.local.language.bhv.context.{BehaviorStoreContext, ParsedBehavior}

import scala.collection.mutable

class MutableBehaviorStoreContext extends BehaviorStoreContext {

    private val behaviors = mutable.HashMap.empty[String, SystemBehavior]
    private val storedAgreements = mutable.HashMap.empty[String, RMIRulesAgreement]

    def addBehavior(bhv: ParsedBehavior): Unit = {
        addBehavior(bhv.toSystemBehavior(this))
    }

    def addBehavior(bhv: SystemBehavior): Unit = {
        behaviors.put(bhv.getFullName, bhv)
    }

    override def findBehavior(name: String): Option[SystemBehavior] = {
        behaviors.get(name)
    }

    override def findAgreement(name: String): Option[RMIRulesAgreement] = {
        storedAgreements.get(name)
    }
}
