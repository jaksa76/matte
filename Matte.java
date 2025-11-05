///usr/bin/env jbang "$0" "$@" ; exit $?

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

class Field<T> {
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

class Entity {
    final java.util.Map<Object, Field<?>> data = new java.util.HashMap<>();
    final Field<Long> id = field("id", Long.class);

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

class JsonSerializer {
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

class Repository<T extends Entity> {
    private final String name;
    private final Map<Long, T> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Repository(String name) {
        this.name = name;
    }

    public T save(T entity) {
        Long id = entity.id.get();
        if (id == null) {
            id = idGenerator.getAndIncrement();
            entity.id.set(id);
        }
        store.put(id, entity);
        return entity;
    }

    public T findById(Long id) {
        return store.get(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public int count() {
        return store.size();
    }
}

interface EntityFactory<T extends Entity> {
    T create();
}

class User extends Entity {
    final Field<String> name = field("name", String.class);
    final Field<String> email = field("email", String.class);

    public User() {
        fields(name, email);
    }

    public void getFullDisplay() {
        System.out.println("User{id=" + id.get() + ", name=" + name.get() + ", email=" + email.get() + "}");
    }
}

// Example of another entity that can use the same generic controller
class Product extends Entity {
    final Field<String> name = field("name", String.class);
    final Field<Integer> price = field("price", Integer.class);
    final Field<String> category = field("category", String.class);

    public Product() {
        fields(name, price, category);
    }
}

// Example: Adding a new entity is as simple as:
// 1. Define the entity class
// 2. Register it in the App with .register("resource-name", () -> new EntityClass())
// 
// class Order extends Entity {
//     final Field<Long> userId = field("userId", Long.class);
//     final Field<String> status = field("status", String.class);
//     final Field<Integer> total = field("total", Integer.class);
//     
//     public Order() {
//         fields(userId, status, total);
//     }
// }
//
// Then just add: app.register("orders", () -> new Order());

class EntityController<T extends Entity> {
    private final Repository<T> repository;
    private final String resourceName;
    private final String basePath;
    private final EntityFactory<T> entityFactory;

    public EntityController(Repository<T> repository, String resourceName, EntityFactory<T> entityFactory) {
        this.repository = repository;
        this.resourceName = resourceName;
        this.basePath = "/" + resourceName;
        this.entityFactory = entityFactory;
    }

    public String handleRequest(String method, String path, String body) {
        try {
            if (method.equals("GET") && path.equals(basePath)) {
                return getAll();
            } else if (method.equals("GET") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return getById(Long.parseLong(idStr));
            } else if (method.equals("POST") && path.equals(basePath)) {
                return create(body);
            } else if (method.equals("PUT") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return update(Long.parseLong(idStr), body);
            } else if (method.equals("DELETE") && path.startsWith(basePath + "/")) {
                String idStr = path.substring(basePath.length() + 1);
                return delete(Long.parseLong(idStr));
            } else {
                return errorResponse("Not Found", 404);
            }
        } catch (NumberFormatException e) {
            return errorResponse("Invalid ID format", 400);
        } catch (Exception e) {
            return errorResponse("Internal Server Error: " + e.getMessage(), 500);
        }
    }

    private String getAll() {
        List<T> entities = repository.findAll();
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (T entity : entities) {
            if (!first) json.append(",");
            first = false;
            json.append(JsonSerializer.toJson(entity));
        }
        json.append("]");
        return json.toString();
    }

    private String getById(Long id) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        return JsonSerializer.toJson(entity);
    }

    private String create(String body) {
        T entity = entityFactory.create();
        populateEntityFromJson(entity, body);
        repository.save(entity);
        return JsonSerializer.toJson(entity);
    }

    private String update(Long id, String body) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        populateEntityFromJson(entity, body);
        repository.save(entity);
        return JsonSerializer.toJson(entity);
    }

    private String delete(Long id) {
        T entity = repository.findById(id);
        if (entity == null) {
            return errorResponse(capitalize(resourceName) + " not found", 404);
        }
        repository.deleteById(id);
        return "{\"message\":\"" + capitalize(resourceName) + " deleted successfully\"}";
    }

    private void populateEntityFromJson(T entity, String json) {
        for (Map.Entry<Object, Field<?>> entry : entity.data.entrySet()) {
            Field field = entry.getValue();
            String fieldName = field.fieldName().toString();
            
            // Skip the id field for creation
            if (fieldName.equals("id")) continue;
            
            if (field.type() == String.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    ((Field<String>) field).set(value);
                }
            } else if (field.type() == Integer.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    try {
                        ((Field<Integer>) field).set(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Skip invalid integers
                    }
                }
            } else if (field.type() == Long.class && !fieldName.equals("id")) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    try {
                        ((Field<Long>) field).set(Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        // Skip invalid longs
                    }
                }
            } else if (field.type() == Boolean.class) {
                String value = extractJsonValue(json, fieldName);
                if (value != null) {
                    ((Field<Boolean>) field).set(Boolean.parseBoolean(value));
                }
            }
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) return null;
        valueStart++;
        
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String errorResponse(String message, int statusCode) {
        return "{\"error\":\"" + message + "\",\"status\":" + statusCode + "}";
    }
}

class App {
    private final Map<String, EntityController<?>> controllers = new HashMap<>();
    private final Map<String, Repository<?>> repositories = new HashMap<>();
    private HttpServer server;
    private final int port;

    public App() {
        this(8080);
    }

    public App(int port) {
        this.port = port;
    }

    public <T extends Entity> App register(String resourceName, EntityFactory<T> factory) {
        // Create repository and controller for this entity
        Repository<T> repository = new Repository<>(resourceName);
        EntityController<T> controller = new EntityController<>(repository, resourceName, factory);
        
        // Store them
        controllers.put(resourceName, controller);
        repositories.put(resourceName, repository);
        
        System.out.println("✅ Registered entity: " + resourceName);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Repository<T> getRepository(String resourceName) {
        return (Repository<T>) repositories.get(resourceName);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityController<T> getController(String resourceName) {
        return (EntityController<T>) controllers.get(resourceName);
    }

    public App start() throws IOException {
        if (controllers.isEmpty()) {
            System.out.println("⚠️  No entities registered. Register entities before starting the server.");
            return this;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Create context for each registered entity
        for (Map.Entry<String, EntityController<?>> entry : controllers.entrySet()) {
            String resourceName = entry.getKey();
            EntityController<?> controller = entry.getValue();
            
            server.createContext("/" + resourceName, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String method = exchange.getRequestMethod();
                    String path = exchange.getRequestURI().getPath();
                    
                    String body = "";
                    if (method.equals("POST") || method.equals("PUT")) {
                        body = new String(exchange.getRequestBody().readAllBytes());
                    }
                    
                    String response = controller.handleRequest(method, path, body);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });
        }

        server.setExecutor(null);
        server.start();

        System.out.println("\n🚀 Server started on http://localhost:" + port);
        printEndpoints();
        
        return this;
    }

    private void printEndpoints() {
        for (String resourceName : controllers.keySet()) {
            System.out.println("\n📋 " + capitalize(resourceName) + " endpoints:");
            System.out.println("  GET    /" + resourceName + "          - Get all " + resourceName);
            System.out.println("  GET    /" + resourceName + "/{id}     - Get " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
            System.out.println("  POST   /" + resourceName + "          - Create new " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  PUT    /" + resourceName + "/{id}     - Update " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  DELETE /" + resourceName + "/{id}     - Delete " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
        }
        
        System.out.println("\n📝 Example commands:");
        String firstResource = controllers.keySet().iterator().next();
        System.out.println("  curl http://localhost:" + port + "/" + firstResource);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Server stopped");
        }
    }
}

public class Matte {
  public static void main(String[] args) throws IOException {
    // Create the app and register entities
    App app = new App(8080)
        .register("users", () -> new User())
        .register("products", () -> new Product());

    // Get repositories to add sample data
    Repository<User> userRepo = app.getRepository("users");
    Repository<Product> productRepo = app.getRepository("products");

    // Add sample users
    User user1 = new User();
    user1.name.set("John Doe");
    user1.email.set("john.doe@example.com");
    userRepo.save(user1);

    User user2 = new User();
    user2.name.set("Jane Smith");
    user2.email.set("jane.smith@example.com");
    userRepo.save(user2);

    // Add sample products
    Product product1 = new Product();
    product1.name.set("Laptop");
    product1.price.set(999);
    product1.category.set("Electronics");
    productRepo.save(product1);

    Product product2 = new Product();
    product2.name.set("Coffee Mug");
    product2.price.set(15);
    product2.category.set("Kitchen");
    productRepo.save(product2);

    System.out.println("Sample users created:");
    System.out.println(JsonSerializer.toJson(user1));
    System.out.println(JsonSerializer.toJson(user2));
    
    System.out.println("\nSample products created:");
    System.out.println(JsonSerializer.toJson(product1));
    System.out.println(JsonSerializer.toJson(product2));

    // Start the server
    app.start();
  }
}
