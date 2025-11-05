///usr/bin/env jbang "$0" "$@" ; exit $?

import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.io.IOException;

public class hello {
    public static void main(String... args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/hello", exchange -> {
            String response = "Hello World!";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:8080/hello");
    }
}
