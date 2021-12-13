package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.MethodControl

trait UserWallet {

    val owner: UserAccount

    @MethodControl(ONLY_CACHE_OWNER)
    val name: String

    @MethodControl(ONLY_CACHE_OWNER)
    def sendMoney(amount: Int, to: UserWallet): Unit

}
