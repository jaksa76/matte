///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.nanohttpd:nanohttpd:2.3.1
//JAVA 17+

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

// ============= Fluent Framework Core =============

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
    
    @SuppressWarnings("unchecked")
    public Object getId(T instance) {
        return idField != null ? idField.get(instance) : null;
    }
}

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
    
    public static <T> String toJsonArray(List<T> instances, EntitySchema<T> schema) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (T instance : instances) {
            if (!first) json.append(",");
            first = false;
            json.append(toJson(instance, schema));
        }
        
        json.append("]");
        return json.toString();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, EntitySchema<T> schema) {
        T instance = schema.newInstance();
        
        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1, content.length() - 1);
        }
        
        Map<String, String> values = new HashMap<>();
        
        // Simple parser - handles basic JSON
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = false;
        boolean inValue = false;
        boolean inString = false;
        
        for (char c : content.toCharArray()) {
            if (c == '"' && !inString) {
                inString = true;
                if (!inKey && !inValue) inKey = true;
                else if (inKey) { inKey = false; }
                else if (inValue) { 
                    inValue = false;
                    values.put(key.toString(), value.toString());
                    key = new StringBuilder();
                    value = new StringBuilder();
                }
            } else if (c == '"' && inString) {
                inString = false;
            } else if (c == ':' && !inString) {
                inValue = true;
            } else if (c == ',' && !inString) {
                if (inValue) {
                    values.put(key.toString(), value.toString());
                    key = new StringBuilder();
                    value = new StringBuilder();
                    inValue = false;
                }
            } else if (inString) {
                if (inKey) key.append(c);
                else if (inValue) value.append(c);
            } else if (inValue && c != ' ') {
                value.append(c);
            }
        }
        
        if (key.length() > 0 && value.length() > 0) {
            values.put(key.toString(), value.toString());
        }
        
        for (Field<T, ?> field : schema.getFields()) {
            String val = values.get(field.getName());
            if (val != null && !val.equals("null")) {
                if (field.getType() == Long.class) {
                    ((Field<T, Long>) field).set(instance, Long.parseLong(val));
                } else if (field.getType() == String.class) {
                    ((Field<T, String>) field).set(instance, val);
                } else if (field.getType() == Integer.class) {
                    ((Field<T, Integer>) field).set(instance, Integer.parseInt(val));
                }
            }
        }
        
        return instance;
    }
    
    private static String escape(String str) {
        return str == null ? "" : str.replace("\"", "\\\"").replace("\n", "\\n");
    }
}

// ============= In-Memory Repository =============

class Repository<T> {
    private final EntitySchema<T> schema;
    private final Map<Object, T> store = new ConcurrentHashMap<>();
    
    public Repository(EntitySchema<T> schema) {
        this.schema = schema;
    }
    
    public T save(T entity) {
        Object id = schema.getId(entity);
        store.put(id, entity);
        return entity;
    }
    
    public Optional<T> findById(Object id) {
        return Optional.ofNullable(store.get(id));
    }
    
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }
    
    public boolean deleteById(Object id) {
        return store.remove(id) != null;
    }
    
    public EntitySchema<T> getSchema() {
        return schema;
    }
}

// ============= REST Controller =============

class RestController<T> {
    private final Repository<T> repository;
    private final String basePath;
    
    public RestController(Repository<T> repository, String basePath) {
        this.repository = repository;
        this.basePath = basePath;
    }
    
    public NanoHTTPD.Response handleRequest(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();
        
        try {
            // GET /users - list all
            if (method.equals("GET") && uri.equals(basePath)) {
                List<T> all = repository.findAll();
                String json = JsonSerializer.toJsonArray(all, repository.getSchema());
                return newJsonResponse(NanoHTTPD.Response.Status.OK, json);
            }
            
            // GET /users/123 - get by id
            if (method.equals("GET") && uri.startsWith(basePath + "/")) {
                String idStr = uri.substring(basePath.length() + 1);
                Long id = Long.parseLong(idStr);
                Optional<T> entity = repository.findById(id);
                
                if (entity.isPresent()) {
                    String json = JsonSerializer.toJson(entity.get(), repository.getSchema());
                    return newJsonResponse(NanoHTTPD.Response.Status.OK, json);
                } else {
                    return newJsonResponse(NanoHTTPD.Response.Status.NOT_FOUND, 
                        "{\"error\":\"Not found\"}");
                }
            }
            
            // POST /users - create
            if (method.equals("POST") && uri.equals(basePath)) {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String body = files.get("postData");
                
                T entity = JsonSerializer.fromJson(body, repository.getSchema());
                T saved = repository.save(entity);
                String json = JsonSerializer.toJson(saved, repository.getSchema());
                return newJsonResponse(NanoHTTPD.Response.Status.CREATED, json);
            }
            
            // DELETE /users/123 - delete
            if (method.equals("DELETE") && uri.startsWith(basePath + "/")) {
                String idStr = uri.substring(basePath.length() + 1);
                Long id = Long.parseLong(idStr);
                boolean deleted = repository.deleteById(id);
                
                if (deleted) {
                    return newJsonResponse(NanoHTTPD.Response.Status.OK, 
                        "{\"message\":\"Deleted\"}");
                } else {
                    return newJsonResponse(NanoHTTPD.Response.Status.NOT_FOUND, 
                        "{\"error\":\"Not found\"}");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return newJsonResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, 
                "{\"error\":\"" + e.getMessage() + "\"}");
        }
        
        return newJsonResponse(NanoHTTPD.Response.Status.NOT_FOUND, 
            "{\"error\":\"Not found\"}");
    }
    
    private NanoHTTPD.Response newJsonResponse(NanoHTTPD.Response.Status status, String json) {
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(status, 
            "application/json", json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
}

// ============= Application Server =============

class AppServer extends NanoHTTPD {
    private final Map<String, RestController<?>> controllers = new HashMap<>();
    
    public AppServer(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }
    
    public <T> void registerController(String basePath, RestController<T> controller) {
        controllers.put(basePath, controller);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        // Root endpoint
        if (uri.equals("/")) {
            String info = "{\"message\":\"Fluent Framework API\",\"endpoints\":[" +
                String.join(",", controllers.keySet().stream()
                    .map(p -> "\"" + p + "\"")
                    .toArray(String[]::new)) + "]}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", info);
        }
        
        // Find matching controller
        for (Map.Entry<String, RestController<?>> entry : controllers.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue().handleRequest(session);
            }
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, 
            "application/json", "{\"error\":\"Not found\"}");
    }
}

// ============= Domain Models =============

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

// ============= Main Application =============

public class fluent_server {
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Starting Fluent Framework Server...\n");
        
        // Define schemas
        EntitySchema<User> userSchema = new EntitySchema<>("users", User::new)
            .idField("id", Long.class, User::getId, User::setId)
            .field("name", String.class, User::getName, User::setName)
            .field("email", String.class, User::getEmail, User::setEmail);
        
        EntitySchema<Product> productSchema = new EntitySchema<>("products", Product::new)
            .idField("id", Long.class, Product::getId, Product::setId)
            .field("name", String.class, Product::getName, Product::setName)
            .field("price", Integer.class, Product::getPrice, Product::setPrice);
        
        // Create repositories
        Repository<User> userRepo = new Repository<>(userSchema);
        Repository<Product> productRepo = new Repository<>(productSchema);
        
        // Add sample data
        userRepo.save(new User(1L, "Alice", "alice@example.com"));
        userRepo.save(new User(2L, "Bob", "bob@example.com"));
        productRepo.save(new Product(1L, "Widget", 999));
        productRepo.save(new Product(2L, "Gadget", 1499));
        
        // Create controllers
        RestController<User> userController = new RestController<>(userRepo, "/users");
        RestController<Product> productController = new RestController<>(productRepo, "/products");
        
        // Start server
        AppServer server = new AppServer(8080);
        server.registerController("/users", userController);
        server.registerController("/products", productController);
        
        System.out.println("✅ Server running on http://localhost:8080");
        System.out.println("\n📋 Available endpoints:");
        System.out.println("  GET    http://localhost:8080/          - API info");
        System.out.println("  GET    http://localhost:8080/users     - List all users");
        System.out.println("  GET    http://localhost:8080/users/1   - Get user by ID");
        System.out.println("  POST   http://localhost:8080/users     - Create user");
        System.out.println("  DELETE http://localhost:8080/users/1   - Delete user");
        System.out.println("  GET    http://localhost:8080/products  - List all products");
        System.out.println("  GET    http://localhost:8080/products/1 - Get product by ID");
        System.out.println("\n✨ Press Ctrl+C to stop\n");
        
        // Keep running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("\n👋 Server stopped");
        }
    }
}
