///usr/bin/env jbang "$0" "$@" ; exit $?

import static java.lang.System.*;

class SimpleHello {
    public static void main(String... args) {
        out.println("Hello from native executable!");
        out.println("JBang + GraalVM Native Image works!");
        
        if (args.length > 0) {
            out.println("Arguments: " + String.join(", ", args));
        }
    }
}
