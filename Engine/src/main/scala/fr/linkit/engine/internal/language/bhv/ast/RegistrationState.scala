package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel

/**
 * @param forced: true if the user explicitly specified this state.
 * @param lvl the synchronisation level involved.
 * */
case class RegistrationState(forced: Boolean, lvl: SyncLevel)

