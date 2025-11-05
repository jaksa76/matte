///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.10.1
//JAVA 17+

import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import com.google.gson.Gson;
import java.util.Map;

public class enhanced_hello {
    private static final Gson gson = new Gson();
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Simple text response
        server.createContext("/hello", exchange -> {
            String response = "Hello World!";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        
        // JSON response
        server.createContext("/api/hello", exchange -> {
            Map<String, Object> data = Map.of(
                "message", "Hello World!",
                "framework", "JDK HttpServer",
                "status", "success"
            );
            String json = gson.toJson(data);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        });
        
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:8080");
        System.out.println("  - Text endpoint: http://localhost:8080/hello");
        System.out.println("  - JSON endpoint: http://localhost:8080/api/hello");
    }
}
