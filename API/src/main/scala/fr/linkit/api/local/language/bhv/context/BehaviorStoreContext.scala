package fr.linkit.api.local.language.bhv.context

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, SystemBehavior}

trait BehaviorStoreContext {

    def findBehavior(fullName: String): Option[SystemBehavior]

    def findAgreement(name: String): Option[RMIRulesAgreement]

}
