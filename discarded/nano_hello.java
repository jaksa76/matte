///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.nanohttpd:nanohttpd:2.3.1
//JAVA 17+

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

public class nano_hello extends NanoHTTPD {
    public nano_hello() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server started on http://localhost:8080/hello");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.equals("/hello")) {
            return newFixedLengthResponse("Hello World from NanoHTTPD!");
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    public static void main(String[] args) throws IOException {
        new nano_hello();
        // Keep the server running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
