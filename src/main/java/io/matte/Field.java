package io.matte;

public class Field<T> {
    private final Object fieldName;
    private final Class<T> type;
    private T value;

    public Field(Object fieldName, Class<T> type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public Object fieldName() {
        return fieldName;
    }

    public Class<T> type() {
        return type;
    }
}
