///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.undertow:undertow-core:2.3.10.Final
//JAVA 17+

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class undertow_hello {
    public static void main(String[] args) {
        Undertow server = Undertow.builder()
            .addHttpListener(8080, "0.0.0.0")
            .setHandler(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    if (exchange.getRequestPath().equals("/hello")) {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("Hello World from Undertow!");
                    } else {
                        exchange.setStatusCode(404);
                        exchange.getResponseSender().send("Not Found");
                    }
                }
            })
            .build();
        server.start();
        System.out.println("Server started on http://localhost:8080/hello");
    }
}
