package fr.linkit.engine.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.connection.cache.obj.description.FieldDescription
import org.jetbrains.annotations.Nullable

case class SyncFieldBehavior[F](desc: FieldDescription,
                                override val isActivated: Boolean,
                                @Nullable override val modifier: FieldModifier[F]) extends FieldBehavior[F] {
    override def getName: String = desc.javaField.getName

}
