///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.micronaut:micronaut-http-server-netty:4.2.3
//DEPS io.micronaut:micronaut-runtime:4.2.3
//DEPS io.micronaut:micronaut-inject:4.2.3
//DEPS ch.qos.logback:logback-classic:1.4.14
//JAVA 17+
//NATIVE_OPTIONS --initialize-at-build-time=ch.qos.logback,org.slf4j
//NATIVE_OPTIONS -H:+ReportExceptionStackTraces
//NATIVE_OPTIONS --no-fallback

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.Micronaut;

@Controller
class HelloController {
    @Get("/hello")
    String hello() {
        return "Hello World from Micronaut!";
    }
}

public class micronaut_hello {
    public static void main(String[] args) {
        Micronaut.run(micronaut_hello.class, args);
    }
}
