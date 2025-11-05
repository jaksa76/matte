///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.http4k:http4k-core:5.10.2.0
//DEPS org.http4k:http4k-server-netty:5.10.2.0
//JAVA 17+

import org.http4k.core.*;
import org.http4k.server.Netty;
import org.http4k.server.NettyKt;

public class http4k_hello {
    public static void main(String[] args) {
        HttpHandler app = (Request request) -> {
            if (request.getUri().getPath().equals("/hello")) {
                return Response.Companion.ok("Hello World from http4k!");
            }
            return Response.Companion.status(Status.Companion.getNOT_FOUND());
        };
        
        NettyKt.asServer(app, Netty.Companion.invoke(8080)).start();
        System.out.println("Server started on http://localhost:8080/hello");
    }
}
