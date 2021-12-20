package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{FullRemote, MethodControl}

@FullRemote(ONLY_CACHE_OWNER)
trait CurrentUserAccount extends UserAccount {

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def openWallet(name: String, initialAmount: Int): CurrentUserWallet

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    override def findWallet(name: String): CurrentUserWallet

}
