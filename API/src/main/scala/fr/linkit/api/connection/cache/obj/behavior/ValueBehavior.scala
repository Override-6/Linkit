package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.field.FieldBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.returnvalue.ReturnValueBehavior

trait ValueBehavior[A] {

    def whenField: Option[FieldBehavior[A]]

    def whenParameter: Option[ParameterBehavior[A]]

    def whenMethodReturnValue: Option[ReturnValueBehavior[A]]
}
