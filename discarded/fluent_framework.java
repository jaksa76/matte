///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

import java.util.*;
import java.util.function.*;

// Field definition with type information
class Field<T, V> {
    private final String name;
    private final Class<V> type;
    private final Function<T, V> getter;
    private final BiConsumer<T, V> setter;
    
    public Field(String name, Class<V> type, Function<T, V> getter, BiConsumer<T, V> setter) {
        this.name = name;
        this.type = type;
        this.getter = getter;
        this.setter = setter;
    }
    
    public String getName() { return name; }
    public Class<V> getType() { return type; }
    public V get(T instance) { return getter.apply(instance); }
    public void set(T instance, V value) { setter.accept(instance, value); }
}

// Entity schema defined fluently
class EntitySchema<T> {
    private final String tableName;
    private final Supplier<T> constructor;
    private final List<Field<T, ?>> fields = new ArrayList<>();
    private Field<T, ?> idField;
    
    public EntitySchema(String tableName, Supplier<T> constructor) {
        this.tableName = tableName;
        this.constructor = constructor;
    }
    
    public <V> EntitySchema<T> field(String name, Class<V> type, 
                                     Function<T, V> getter, BiConsumer<T, V> setter) {
        fields.add(new Field<>(name, type, getter, setter));
        return this;
    }
    
    public <V> EntitySchema<T> idField(String name, Class<V> type,
                                       Function<T, V> getter, BiConsumer<T, V> setter) {
        this.idField = new Field<>(name, type, getter, setter);
        fields.add(this.idField);
        return this;
    }
    
    public T newInstance() { return constructor.get(); }
    public String getTableName() { return tableName; }
    public List<Field<T, ?>> getFields() { return fields; }
    public Field<T, ?> getIdField() { return idField; }
}

// Generic JSON serializer that works with any schema
class JsonSerializer {
    public static <T> String toJson(T instance, EntitySchema<T> schema) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Field<T, ?> field : schema.getFields()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(field.getName()).append("\":");
            Object value = field.get(instance);
            
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value == null) {
                json.append("null");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, EntitySchema<T> schema) {
        T instance = schema.newInstance();
        
        // Simple JSON parsing (use proper parser in production)
        String content = json.substring(1, json.length() - 1);
        Map<String, String> values = new HashMap<>();
        
        for (String pair : content.split(",")) {
            String[] kv = pair.split(":", 2);
            String key = kv[0].replaceAll("\"", "").trim();
            String value = kv[1].replaceAll("\"", "").trim();
            values.put(key, value);
        }
        
        for (Field<T, ?> field : schema.getFields()) {
            String value = values.get(field.getName());
            if (value != null && !value.equals("null")) {
                if (field.getType() == Long.class) {
                    ((Field<T, Long>) field).set(instance, Long.parseLong(value));
                } else if (field.getType() == String.class) {
                    ((Field<T, String>) field).set(instance, value);
                } else if (field.getType() == Integer.class) {
                    ((Field<T, Integer>) field).set(instance, Integer.parseInt(value));
                }
            }
        }
        
        return instance;
    }
    
    private static String escape(String str) {
        return str == null ? "" : str.replace("\"", "\\\"");
    }
}

// Generic SQL generator
class SqlGenerator {
    public static <T> String insert(T instance, EntitySchema<T> schema) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        
        for (Field<T, ?> field : schema.getFields()) {
            if (!first) {
                cols.append(", ");
                vals.append(", ");
            }
            first = false;
            
            cols.append(field.getName());
            Object value = field.get(instance);
            
            if (value instanceof String) {
                vals.append("'").append(((String) value).replace("'", "''")).append("'");
            } else {
                vals.append(value);
            }
        }
        
        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                           schema.getTableName(), cols, vals);
    }
    
    public static <T> String selectById(Object id, EntitySchema<T> schema) {
        StringBuilder cols = new StringBuilder();
        boolean first = true;
        
        for (Field<T, ?> field : schema.getFields()) {
            if (!first) cols.append(", ");
            first = false;
            cols.append(field.getName());
        }
        
        return String.format("SELECT %s FROM %s WHERE %s = %s",
                           cols, schema.getTableName(), 
                           schema.getIdField().getName(), id);
    }
}

// Simple entity class - just a POJO
class User {
    private Long id;
    private String name;
    private String email;
    
    public User() {}
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

public class fluent_framework {
    public static void main(String[] args) {
        System.out.println("=== Fluent API Framework (No Reflection!) ===\n");
        
        // Define the schema using fluent API
        EntitySchema<User> userSchema = new EntitySchema<>("users", User::new)
            .idField("id", Long.class, User::getId, User::setId)
            .field("name", String.class, User::getName, User::setName)
            .field("email", String.class, User::getEmail, User::setEmail);
        
        // Create entity
        User user = new User(42L, "Alice Johnson", "alice@example.com");
        
        // Generic serialization - works for ANY entity with a schema!
        String json = JsonSerializer.toJson(user, userSchema);
        System.out.println("JSON: " + json);
        
        // Generic deserialization
        User parsed = JsonSerializer.fromJson(json, userSchema);
        System.out.println("Parsed: " + parsed.getName() + " (" + parsed.getEmail() + ")");
        
        // Generic SQL generation
        System.out.println("SQL Insert: " + SqlGenerator.insert(user, userSchema));
        System.out.println("SQL Select: " + SqlGenerator.selectById(42L, userSchema));
        
        // Add another entity type to show it's generic
        System.out.println("\n--- Adding Product entity ---");
        
        EntitySchema<Product> productSchema = new EntitySchema<>("products", Product::new)
            .idField("id", Long.class, Product::getId, Product::setId)
            .field("name", String.class, Product::getName, Product::setName)
            .field("price", Integer.class, Product::getPrice, Product::setPrice);
        
        Product product = new Product(1L, "Widget", 999);
        System.out.println("Product JSON: " + JsonSerializer.toJson(product, productSchema));
        System.out.println("Product SQL: " + SqlGenerator.insert(product, productSchema));
        
        System.out.println("\n✅ No reflection!");
        System.out.println("✅ No annotations!");
        System.out.println("✅ Fully generic serialization!");
        System.out.println("✅ Type-safe with compile-time checking!");
        System.out.println("✅ Native image ready!");
    }
}

// Another entity to demonstrate genericity
class Product {
    private Long id;
    private String name;
    private Integer price;
    
    public Product() {}
    public Product(Long id, String name, Integer price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}
