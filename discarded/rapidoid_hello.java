///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.rapidoid:rapidoid-http-fast:5.5.5
//JAVA 17+

import org.rapidoid.setup.On;

public class rapidoid_hello {
    public static void main(String[] args) {
        On.get("/hello").json(() -> "Hello World from Rapidoid!");
        System.out.println("Server started on http://localhost:8080/hello");
    }
}
