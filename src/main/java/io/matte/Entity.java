package io.matte;

import java.util.Map;
import java.util.HashMap;

public class Entity {
    final Map<Object, Field<?>> data = new HashMap<>();
    public final Field<Long> id = field("id", Long.class);

    public Entity() {
        data.put(id.fieldName(), id);
    }

    protected static <T> Field<T> field(Object fieldName, Class<T> type) {
        Field<T> entityField = new Field<T>(fieldName, type);
        return entityField;
    }

    protected void fields(Field<?>... fields) {
        for (Field<?> field : fields) {
            data.put(field.fieldName(), field);
        }
    }
}
