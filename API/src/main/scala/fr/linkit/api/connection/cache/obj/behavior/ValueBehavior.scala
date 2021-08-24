package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldModifier
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterModifier
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueModifier

trait ValueBehavior[A] {

    def whenField: Option[FieldModifier[A]]

    def whenParameter: Option[ParameterModifier[A]]

    def whenMethodReturnValue: Option[ReturnValueModifier[A]]
}
