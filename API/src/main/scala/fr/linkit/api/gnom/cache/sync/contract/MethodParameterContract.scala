package fr.linkit.api.gnom.cache.sync.contract

import java.lang.reflect.Parameter

import fr.linkit.api.gnom.cache.sync.contract.modification.MethodCompModifier
import org.jetbrains.annotations.Nullable

trait MethodParameterContract {

              val param   : Parameter
    @Nullable val modifier: MethodCompModifier[AnyRef]

}
