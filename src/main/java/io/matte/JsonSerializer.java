package io.matte;

public class JsonSerializer {
    public static <T extends Entity> String toJson(T instance) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;

        for (Field field : instance.data.values()) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(field.fieldName()).append("\":");

            Object value = field.get();
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else {
                json.append(value);
            }
        }

        json.append("}");
        return json.toString();
    }

    private static String escape(String str) {
        return str.replace("\"", "\\\"");
    }
}
