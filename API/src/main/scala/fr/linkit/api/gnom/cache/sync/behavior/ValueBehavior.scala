package fr.linkit.api.gnom.cache.sync.behavior

import fr.linkit.api.gnom.cache.sync.behavior.member.field.FieldModifier
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterModifier
import fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue.ReturnValueModifier

trait ValueBehavior[A] {

    def whenField: Option[FieldModifier[A]]

    def whenParameter: Option[ParameterModifier[A]]

    def whenMethodReturnValue: Option[ReturnValueModifier[A]]
}
