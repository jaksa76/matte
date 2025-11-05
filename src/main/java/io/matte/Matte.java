package io.matte;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Matte {
    private final Map<String, EntityController<?>> controllers = new HashMap<>();
    private final Map<String, Repository<?>> repositories = new HashMap<>();
    private HttpServer server;
    private final int port;

    public Matte() {
        this(8080);
    }

    public Matte(int port) {
        this.port = port;
    }

    public <T extends Entity> Matte register(String resourceName, EntityFactory<T> factory) {
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

    public Matte start() throws IOException {
        if (controllers.isEmpty()) {
            System.out.println("⚠️  No entities registered. Register entities before starting the server.");
            return this;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Serve static files and root path
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                
                // Redirect root to index.html
                if (path.equals("/")) {
                    path = "/index.html";
                }
                
                // Handle static files
                if (path.startsWith("/static/") || path.equals("/index.html")) {
                    serveStaticFile(exchange, path);
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        });

        // API endpoint to get list of registered entities
        server.createContext("/api/entities", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                
                String json = "[" + String.join(",", 
                    controllers.keySet().stream()
                        .map(name -> "\"" + name + "\"")
                        .collect(Collectors.toList())
                ) + "]";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(json.getBytes());
                os.close();
            }
        });

        // Create context for each registered entity
        for (Map.Entry<String, EntityController<?>> entry : controllers.entrySet()) {
            String resourceName = entry.getKey();
            EntityController<?> controller = entry.getValue();
            
            server.createContext("/api/" + resourceName, new HttpHandler() {
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
    
    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        // Remove leading slash from path
        String resourcePath = path.startsWith("/") ? path.substring(1) : path;
        
        // For index.html, look in static/ directory
        if (resourcePath.equals("index.html")) {
            resourcePath = "static/index.html";
        }
        
        // Path already contains "static/" prefix, use as-is
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (is == null) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
            return;
        }
        
        // Determine content type
        String contentType = "text/plain";
        if (resourcePath.endsWith(".html")) {
            contentType = "text/html";
        } else if (resourcePath.endsWith(".css")) {
            contentType = "text/css";
        } else if (resourcePath.endsWith(".js")) {
            contentType = "application/javascript";
        }
        
        byte[] content = is.readAllBytes();
        is.close();
        
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private void printEndpoints() {
        for (String resourceName : controllers.keySet()) {
            System.out.println("\n📋 " + capitalize(resourceName) + " endpoints:");
            System.out.println("  GET    /api/" + resourceName + "          - Get all " + resourceName);
            System.out.println("  GET    /api/" + resourceName + "/{id}     - Get " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
            System.out.println("  POST   /api/" + resourceName + "          - Create new " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  PUT    /api/" + resourceName + "/{id}     - Update " + resourceName.substring(0, resourceName.length() - 1));
            System.out.println("  DELETE /api/" + resourceName + "/{id}     - Delete " + resourceName.substring(0, resourceName.length() - 1) + " by ID");
        }
        
        System.out.println("\n📝 Example commands:");
        String firstResource = controllers.keySet().iterator().next();
        System.out.println("  curl http://localhost:" + port + "/api/" + firstResource);
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
