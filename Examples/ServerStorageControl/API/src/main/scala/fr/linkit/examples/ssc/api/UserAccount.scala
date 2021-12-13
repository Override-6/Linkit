package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.MethodControl
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER

trait UserAccount {

    val name: String

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def getWallets: Seq[UserWallet]

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def findWallet(name: String): Option[UserWallet]

}
