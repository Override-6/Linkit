package fr.linkit.engine.connection.packet.persistence.context.profile.persistence;

import fr.linkit.api.connection.packet.persistence.context.TypePersistence;
import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure;
import fr.linkit.engine.connection.packet.persistence.context.structure.ClassObjectStructure;

public class NativeTypePersistence<T> implements TypePersistence<T> {

    private final ObjectStructure structure;

    public NativeTypePersistence(Class<?> clazz) {
        this.structure = ClassObjectStructure.apply(clazz);
    }

    @Override
    public ObjectStructure structure() {
        return this.structure;
    }

    @Override
    public native void initInstance(T allocatedObject, Object[] args);

    @Override
    public native Object[] toArray(T t);
}
