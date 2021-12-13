package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.MethodControl

trait CurrentUserAccount extends UserAccount {

    @MethodControl(ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def openWallet(name: String, initialAmount: Int): CurrentUserWallet

    @MethodControl(ONLY_CACHE_OWNER)
    override def findWallet(name: String): Option[CurrentUserWallet]

}
