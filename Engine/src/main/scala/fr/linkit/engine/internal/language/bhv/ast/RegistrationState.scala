package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel

case class RegistrationState(forced: Boolean, kind: SyncLevel)

