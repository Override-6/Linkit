package fr.linkit.engine.gnom.cache.sync.behavior.member

import fr.linkit.api.gnom.cache.sync.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.gnom.cache.sync.description.FieldDescription
import org.jetbrains.annotations.Nullable

case class SyncFieldBehavior[F](desc: FieldDescription,
                                override val isActivated: Boolean,
                                @Nullable override val modifier: FieldModifier[F]) extends FieldBehavior[F] {
    override def getName: String = desc.javaField.getName

}
