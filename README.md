# Matte Framework

A minimal web framework for building REST APIs with ease.

## Building and Running

### Step 1: Build and Install the Framework

First, build the framework and install it to your local Maven repository:

```bash
mvn clean install
```

This will:
- Compile the framework
- Package it as `matte-framework-1.0.0.jar`
- Install it to `~/.m2/repository/io/matte/matte-framework/1.0.0/`

### Step 2: Run the Example Application

Once the framework is installed, you can run the example app with JBang:

```bash
jbang ExampleApp.java
```

The server will start on http://localhost:8080

## Creating Your Own Application

To create your own application using the Matte framework:

1. Create a new JBang script:
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.matte:matte-framework:1.0.0

import io.matte.*;
import java.io.IOException;

class MyEntity extends Entity {
    final Field<String> myField = field("myField", String.class);
    
    public MyEntity() {
        fields(myField);
    }
}

public class MyApp {
    public static void main(String[] args) throws IOException {
        new Matte(8080)
            .register("myentities", () -> new MyEntity())
            .start();
    }
}
```

2. Run it:
```bash
jbang MyApp.java
```

## Development Workflow

### Modifying the Framework

1. Make changes to files in `matte-framework/src/main/java/io/matte/`
2. Rebuild and install: `cd matte-framework && mvn clean install`
3. Your changes will be available to any JBang scripts that depend on the framework

### Publishing the Framework

To publish the framework to Maven Central or a private repository:

1. Update the `<distributionManagement>` section in `pom.xml`
2. Configure authentication in `~/.m2/settings.xml`
3. Run: `mvn clean deploy`

Once published, anyone can use the framework in their JBang scripts without needing to build it locally.

## Benefits of This Architecture

✅ **Framework as Library**: The core framework is a proper Maven artifact  
✅ **JBang Simplicity**: Applications remain simple, single-file scripts  
✅ **Version Control**: Framework can be versioned and distributed independently  
✅ **Easy Updates**: Update framework version by changing the `//DEPS` line  
✅ **No Boilerplate**: Application code focuses only on business logic  
✅ **Rapid Development**: Instant feedback with JBang's fast startup  

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building the framework)
- JBang (for running example applications)

## License

This project is open source and available under the MIT License.
