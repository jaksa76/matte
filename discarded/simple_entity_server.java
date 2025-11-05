///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.nanohttpd:nanohttpd:2.3.1
//JAVA 17+

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

// ============= Generic Entity System =============

// Base interface for field enums
interface EntityField {
    String fieldName();
    Class<?> type();
}

// Abstract base class for field enums - eliminates duplication!
abstract class BaseEntityField implements EntityField {
    private final String fieldName;
    private final Class<?> fieldType;
    
    protected BaseEntityField(String fieldName, Class<?> fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }
    
    @Override
    public String fieldName() {
        return fieldName;
    }
    
    @Override
    public Class<?> type() {
        return fieldType;
    }
}

// Generic entity that stores data in a map
class Entity {
    private final Map<String, Object> data = new HashMap<>();
    
    public <T> T get(EntityField field) {
        @SuppressWarnings("unchecked")
        T value = (T) data.get(field.fieldName());
        return value;
    }
    
    public <T> Entity set(EntityField field, T value) {
        data.put(field.fieldName(), value);
        return this;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public Set<String> getFieldNames() {
        return data.keySet();
    }
}

// Schema definition using enums
class EntitySchema<E extends Enum<E> & EntityField> {
    private final String tableName;
    private final Class<E> fieldsEnum;
    private final E idField;
    private final Supplier<Entity> entityFactory;
    
    public EntitySchema(String tableName, Class<E> fieldsEnum, E idField) {
        this(tableName, fieldsEnum, idField, Entity::new);
    }
    
    public EntitySchema(String tableName, Class<E> fieldsEnum, E idField, 
                       Supplier<Entity> entityFactory) {
        this.tableName = tableName;
        this.fieldsEnum = fieldsEnum;
        this.idField = idField;
        this.entityFactory = entityFactory;
    }
    
    public Entity newInstance() {
        return entityFactory.get();
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public E[] getFields() {
        return fieldsEnum.getEnumConstants();
    }
    
    public E getIdField() {
        return idField;
    }
    
    public Object getId(Entity entity) {
        return entity.get(idField);
    }
}

// Enhanced entity with custom methods
class UserEntity extends Entity {
    public String getFullDisplay() {
        return get(UserFields.NAME) + " <" + get(UserFields.EMAIL) + ">";
    }
    
    public boolean isAdmin() {
        String email = get(UserFields.EMAIL);
        return email != null && email.endsWith("@admin.com");
    }
}

// Field definitions - much cleaner now with base class!
enum UserFields implements EntityField {
    ID("id", Long.class),
    NAME("name", String.class),
    EMAIL("email", String.class);
    
    private final BaseEntityField delegate;
    
    UserFields(String fieldName, Class<?> fieldType) {
        this.delegate = new BaseEntityField(fieldName, fieldType) {};
    }
    
    @Override
    public String fieldName() {
        return delegate.fieldName();
    }
    
    @Override
    public Class<?> type() {
        return delegate.type();
    }
}

enum ProductFields implements EntityField {
    ID("id", Long.class),
    NAME("name", String.class),
    PRICE("price", Integer.class),
    CATEGORY("category", String.class);
    
    private final BaseEntityField delegate;
    
    ProductFields(String fieldName, Class<?> fieldType) {
        this.delegate = new BaseEntityField(fieldName, fieldType) {};
    }
    
    @Override
    public String fieldName() {
        return delegate.fieldName();
    }
    
    @Override
    public Class<?> type() {
        return delegate.type();
    }
}

// Even simpler: Order fields
enum OrderFields implements EntityField {
    ID("id", Long.class),
    CUSTOMER_NAME("customer_name", String.class),
    TOTAL("total", Integer.class),
    STATUS("status", String.class);
    
    private final BaseEntityField delegate;
    
    OrderFields(String fieldName, Class<?> fieldType) {
        this.delegate = new BaseEntityField(fieldName, fieldType) {};
    }
    
    @Override
    public String fieldName() {
        return delegate.fieldName();
    }
    
    @Override
    public Class<?> type() {
        return delegate.type();
    }
}

// ============= Generic Serialization =============

class JsonSerializer {
    public static <E extends Enum<E> & EntityField> String toJson(Entity entity, 
                                                                   EntitySchema<E> schema) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (E field : schema.getFields()) {
            Object value = entity.get(field);
            if (value == null) continue;
            
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(field.fieldName()).append("\":");
            
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    public static <E extends Enum<E> & EntityField> String toJsonArray(
            List<Entity> entities, EntitySchema<E> schema) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Entity entity : entities) {
            if (!first) json.append(",");
            first = false;
            json.append(toJson(entity, schema));
        }
        
        json.append("]");
        return json.toString();
    }
    
    public static <E extends Enum<E> & EntityField> Entity fromJson(
            String json, EntitySchema<E> schema) {
        Entity entity = schema.newInstance();
        
        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1, content.length() - 1);
        }
        
        Map<String, String> values = parseJson(content);
        
        for (E field : schema.getFields()) {
            String val = values.get(field.fieldName());
            if (val != null && !val.equals("null")) {
                Object value = parseValue(val, field.type());
                entity.set(field, value);
            }
        }
        
        return entity;
    }
    
    private static Map<String, String> parseJson(String content) {
        Map<String, String> values = new HashMap<>();
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
        
        return values;
    }
    
    private static Object parseValue(String val, Class<?> type) {
        if (type == Long.class) return Long.parseLong(val);
        if (type == Integer.class) return Integer.parseInt(val);
        if (type == String.class) return val;
        return val;
    }
    
    private static String escape(String str) {
        return str == null ? "" : str.replace("\"", "\\\"").replace("\n", "\\n");
    }
}

// ============= Generic Repository =============

class Repository<E extends Enum<E> & EntityField> {
    private final EntitySchema<E> schema;
    private final Map<Object, Entity> store = new ConcurrentHashMap<>();
    
    public Repository(EntitySchema<E> schema) {
        this.schema = schema;
    }
    
    public Entity save(Entity entity) {
        Object id = schema.getId(entity);
        store.put(id, entity);
        return entity;
    }
    
    public Optional<Entity> findById(Object id) {
        return Optional.ofNullable(store.get(id));
    }
    
    public List<Entity> findAll() {
        return new ArrayList<>(store.values());
    }
    
    public boolean deleteById(Object id) {
        return store.remove(id) != null;
    }
    
    public EntitySchema<E> getSchema() {
        return schema;
    }
}

// ============= REST Controller =============

class RestController<E extends Enum<E> & EntityField> {
    private final Repository<E> repository;
    private final String basePath;
    
    public RestController(Repository<E> repository, String basePath) {
        this.repository = repository;
        this.basePath = basePath;
    }
    
    public NanoHTTPD.Response handleRequest(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();
        
        try {
            if (method.equals("GET") && uri.equals(basePath)) {
                List<Entity> all = repository.findAll();
                String json = JsonSerializer.toJsonArray(all, repository.getSchema());
                return newJsonResponse(NanoHTTPD.Response.Status.OK, json);
            }
            
            if (method.equals("GET") && uri.startsWith(basePath + "/")) {
                String idStr = uri.substring(basePath.length() + 1);
                Long id = Long.parseLong(idStr);
                Optional<Entity> entity = repository.findById(id);
                
                if (entity.isPresent()) {
                    String json = JsonSerializer.toJson(entity.get(), repository.getSchema());
                    return newJsonResponse(NanoHTTPD.Response.Status.OK, json);
                } else {
                    return newJsonResponse(NanoHTTPD.Response.Status.NOT_FOUND, 
                        "{\"error\":\"Not found\"}");
                }
            }
            
            if (method.equals("POST") && uri.equals(basePath)) {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String body = files.get("postData");
                
                Entity entity = JsonSerializer.fromJson(body, repository.getSchema());
                Entity saved = repository.save(entity);
                String json = JsonSerializer.toJson(saved, repository.getSchema());
                return newJsonResponse(NanoHTTPD.Response.Status.CREATED, json);
            }
            
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
    
    public <E extends Enum<E> & EntityField> void registerController(
            String basePath, RestController<E> controller) {
        controllers.put(basePath, controller);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        if (uri.equals("/")) {
            String info = "{\"message\":\"Generic Entity Framework with Base Class\",\"endpoints\":[" +
                String.join(",", controllers.keySet().stream()
                    .map(p -> "\"" + p + "\"")
                    .toArray(String[]::new)) + "]}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", info);
        }
        
        for (Map.Entry<String, RestController<?>> entry : controllers.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue().handleRequest(session);
            }
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, 
            "application/json", "{\"error\":\"Not found\"}");
    }
}

// ============= Main Application =============

public class simple_entity_server {
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Generic Entity Framework with Base Class\n");
        
        // Define schemas - notice how clean the enum definitions are!
        EntitySchema<UserFields> userSchema = new EntitySchema<>(
            "users", UserFields.class, UserFields.ID, UserEntity::new);
        
        EntitySchema<ProductFields> productSchema = new EntitySchema<>(
            "products", ProductFields.class, ProductFields.ID);
        
        EntitySchema<OrderFields> orderSchema = new EntitySchema<>(
            "orders", OrderFields.class, OrderFields.ID);
        
        // Create repositories
        Repository<UserFields> userRepo = new Repository<>(userSchema);
        Repository<ProductFields> productRepo = new Repository<>(productSchema);
        Repository<OrderFields> orderRepo = new Repository<>(orderSchema);
        
        // Add sample data
        userRepo.save(userSchema.newInstance()
            .set(UserFields.ID, 1L)
            .set(UserFields.NAME, "Alice")
            .set(UserFields.EMAIL, "alice@admin.com"));
        
        productRepo.save(productSchema.newInstance()
            .set(ProductFields.ID, 1L)
            .set(ProductFields.NAME, "Widget")
            .set(ProductFields.PRICE, 999)
            .set(ProductFields.CATEGORY, "Hardware"));
        
        orderRepo.save(orderSchema.newInstance()
            .set(OrderFields.ID, 1L)
            .set(OrderFields.CUSTOMER_NAME, "Alice")
            .set(OrderFields.TOTAL, 999)
            .set(OrderFields.STATUS, "shipped"));
        
        // Create controllers
        RestController<UserFields> userController = new RestController<>(userRepo, "/users");
        RestController<ProductFields> productController = 
            new RestController<>(productRepo, "/products");
        RestController<OrderFields> orderController = 
            new RestController<>(orderRepo, "/orders");
        
        // Start server
        AppServer server = new AppServer(8080);
        server.registerController("/users", userController);
        server.registerController("/products", productController);
        server.registerController("/orders", orderController);
        
        System.out.println("✅ Server running on http://localhost:8080");
        System.out.println("\n📋 Endpoints: /users, /products, /orders");
        System.out.println("\n✨ Benefits of BaseEntityField:");
        System.out.println("  ✅ No duplicated fieldName/type methods in enums");
        System.out.println("  ✅ Cleaner enum definitions");
        System.out.println("  ✅ Same functionality, less boilerplate");
        System.out.println("  ✅ Easy to add new entity types");
        System.out.println("\n✨ Press Ctrl+C to stop\n");
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("\n👋 Server stopped");
        }
    }
}
