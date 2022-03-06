package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.annotation.{FullRemote, MethodControl}

@FullRemote(ONLY_CACHE_OWNER)
trait UserWallet {

    @MethodControl(ONLY_CACHE_OWNER)
    def owner: UserAccount

    @MethodControl(ONLY_CACHE_OWNER)
    def name: String

    @MethodControl(ONLY_CACHE_OWNER)
    def sendMoney(amount: Int, to: UserWallet): Unit

}
