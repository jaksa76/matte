///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jooby:jooby:3.0.8
//DEPS io.jooby:jooby-netty:3.0.8
//JAVA 17+

import io.jooby.Jooby;

public class jooby_hello extends Jooby {
    {
        get("/hello", ctx -> "Hello World from Jooby!");
    }

    public static void main(String[] args) {
        new jooby_hello().start();
    }
}
