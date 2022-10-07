package fr.linkit.examples.ssc.api

import java.sql.Timestamp

case class WalletTransaction(id: Int, amount: Int, from: UserWallet, to: UserWallet, timestamp: Timestamp) {
    override def toString: String = s"($id) $timestamp - from ${from.owner.getName} to ${to.owner.getName} amount: $amount"
}