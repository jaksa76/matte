///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.vertx:vertx-core:4.5.0
//DEPS io.vertx:vertx-web:4.5.0
//JAVA 17+

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class vertx_hello {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        
        router.get("/hello").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "text/plain")
                .end("Hello World from Vert.x!");
        });
        
        server.requestHandler(router).listen(8080, http -> {
            if (http.succeeded()) {
                System.out.println("HTTP server started on port 8080");
            } else {
                System.err.println("Failed to start server: " + http.cause());
            }
        });
    }
}
