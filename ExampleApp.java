///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.matte:matte-framework:1.0.0

import io.matte.*;
import java.io.IOException;

// Application-specific entities

class User extends Entity {
    final Field<String> name = field("name", String.class);
    final Field<String> email = field("email", String.class);

    public User() {
        fields(name, email);
    }

    public void getFullDisplay() {
        System.out.println("User{id=" + id.get() + ", name=" + name.get() + ", email=" + email.get() + "}");
    }
}

class Product extends Entity {
    final Field<String> name = field("name", String.class);
    final Field<Integer> price = field("price", Integer.class);
    final Field<String> category = field("category", String.class);

    public Product() {
        fields(name, price, category);
    }
}

// Example: Adding a new entity is as simple as:
// 1. Define the entity class extending Entity
// 2. Register it in the Matte with .register("resource-name", () -> new EntityClass())
// 
// class Order extends Entity {
//     final Field<Long> userId = field("userId", Long.class);
//     final Field<String> status = field("status", String.class);
//     final Field<Integer> total = field("total", Integer.class);
//     
//     public Order() {
//         fields(userId, status, total);
//     }
// }
//
// Then just add: app.register("orders", () -> new Order());

public class ExampleApp {
  public static void main(String[] args) throws IOException {
    // Create the app and register entities
    Matte app = new Matte(8080)
        .register("users", () -> new User())
        .register("products", () -> new Product());

    // Get repositories to add sample data
    Repository<User> userRepo = app.getRepository("users");
    Repository<Product> productRepo = app.getRepository("products");

    // Add sample users
    User user1 = new User();
    user1.name.set("John Doe");
    user1.email.set("john.doe@example.com");
    userRepo.save(user1);

    User user2 = new User();
    user2.name.set("Jane Smith");
    user2.email.set("jane.smith@example.com");
    userRepo.save(user2);

    // Add sample products
    Product product1 = new Product();
    product1.name.set("Laptop");
    product1.price.set(999);
    product1.category.set("Electronics");
    productRepo.save(product1);

    Product product2 = new Product();
    product2.name.set("Coffee Mug");
    product2.price.set(15);
    product2.category.set("Kitchen");
    productRepo.save(product2);

    System.out.println("Sample users created:");
    System.out.println(JsonSerializer.toJson(user1));
    System.out.println(JsonSerializer.toJson(user2));
    
    System.out.println("\nSample products created:");
    System.out.println(JsonSerializer.toJson(product1));
    System.out.println(JsonSerializer.toJson(product2));

    // Start the server
    app.start();
  }
}
