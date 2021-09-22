package fr.linkit.engine.connection.cache.obj.instantiation;

import fr.linkit.api.connection.cache.obj.instantiation.SyncInstanceGetter;

public class ObjectTypeReplacer<T> implements SyncInstanceGetter<T> {

    private final T theObject;
    private boolean changed = false;

    public ObjectTypeReplacer(T theObject) {
        this.theObject = theObject;
    }

    @Override
    public Class<?> tpeClass() {
        return theObject.getClass();
    }

    @Override
    public T getInstance(Class<T> syncClass) {
        synchronized (theObject) {
            if (changed)
                throw new IllegalStateException("Object type has already been replaced.");
            replaceObjectType(theObject, syncClass);
            changed = true;
            return theObject;
        }
    }

    private static native void replaceObjectType(Object object, Class<?> newType);
}
