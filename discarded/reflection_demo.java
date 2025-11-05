///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//NATIVE_OPTIONS -H:+ReportExceptionStackTraces
//NATIVE_OPTIONS --no-fallback
//NATIVE_OPTIONS -H:ReflectionConfigurationFiles=META-INF/native-image/reflect-config.json

import java.lang.reflect.Method;

// This class will be accessed via reflection
class ReflectionTarget {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

public class reflection_demo {
    public static void main(String[] args) throws Exception {
        // Direct instantiation (no reflection) - always works
        ReflectionTarget direct = new ReflectionTarget();
        System.out.println("Direct call: " + direct.greet("World"));
        
        // Reflection-based call - needs configuration for native image
        Class<?> clazz = Class.forName("ReflectionTarget");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod("greet", String.class);
        String result = (String) method.invoke(instance, "Reflection");
        System.out.println("Reflection call: " + result);
    }
}
