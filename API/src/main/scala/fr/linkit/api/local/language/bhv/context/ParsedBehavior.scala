package fr.linkit.api.local.language.bhv.context

import fr.linkit.api.connection.cache.obj.behavior.SystemBehavior

trait ParsedBehavior {

    def getName: String

    def toSystemBehavior(context: BehaviorStoreContext): SystemBehavior

}
