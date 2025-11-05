package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;

@DisplayName("End-to-End API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatteEndToEndTest {

    static class User extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<String> email = field("email", String.class);

        public User() {
            fields(name, email);
        }
    }

    static class Product extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> price = field("price", Integer.class);

        public Product() {
            fields(name, price);
        }
    }

    private static Matte app;
    private static final int TEST_PORT = 8888;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static HttpClient httpClient;

    @BeforeAll
    static void setUp() throws IOException {
        httpClient = HttpClient.newHttpClient();
        
        app = new Matte(TEST_PORT)
            .register("users", User::new)
            .register("products", Product::new);

        // Add some initial data
        Repository<User> userRepo = app.getRepository("users");
        User user1 = new User();
        user1.name.set("John Doe");
        user1.email.set("john@example.com");
        userRepo.save(user1);

        app.start();
        
        // Give server a moment to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Should get list of registered entities")
    void shouldGetListOfRegisteredEntities() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/entities"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"users\"");
        assertThat(response.body()).contains("\"products\"");
        assertThat(response.headers().firstValue("Content-Type"))
            .isPresent()
            .get().isEqualTo("application/json");
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Should get all users")
    void shouldGetAllUsers() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("[");
        assertThat(response.body()).endsWith("]");
        assertThat(response.body()).contains("\"name\":\"John Doe\"");
        assertThat(response.body()).contains("\"email\":\"john@example.com\"");
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Should get user by id")
    void shouldGetUserById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/1"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"id\":1");
        assertThat(response.body()).contains("\"name\":\"John Doe\"");
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/999"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200); // Still 200 as error is in JSON
        assertThat(response.body()).contains("\"error\"");
        assertThat(response.body()).contains("\"status\":404");
    }

    @Test
    @Order(5)
    @DisplayName("E2E: Should create new user")
    void shouldCreateNewUser() throws IOException, InterruptedException {
        String requestBody = "{\"name\":\"Jane Smith\",\"email\":\"jane@example.com\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Jane Smith\"");
        assertThat(response.body()).contains("\"email\":\"jane@example.com\"");
        assertThat(response.body()).contains("\"id\":2");
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Should update existing user")
    void shouldUpdateExistingUser() throws IOException, InterruptedException {
        String requestBody = "{\"name\":\"John Updated\",\"email\":\"john.updated@example.com\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/1"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"John Updated\"");
        assertThat(response.body()).contains("\"email\":\"john.updated@example.com\"");
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Should verify update persisted")
    void shouldVerifyUpdatePersisted() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/1"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"John Updated\"");
    }

    @Test
    @Order(8)
    @DisplayName("E2E: Should delete user")
    void shouldDeleteUser() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/1"))
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"message\"");
        assertThat(response.body()).contains("deleted successfully");
    }

    @Test
    @Order(9)
    @DisplayName("E2E: Should verify user was deleted")
    void shouldVerifyUserWasDeleted() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users/1"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).contains("\"error\"");
        assertThat(response.body()).contains("not found");
    }

    @Test
    @Order(10)
    @DisplayName("E2E: Should handle products endpoint independently")
    void shouldHandleProductsEndpointIndependently() throws IOException, InterruptedException {
        String requestBody = "{\"name\":\"Laptop\",\"price\":\"999\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/products"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Laptop\"");
        assertThat(response.body()).contains("\"price\":999");
        assertThat(response.body()).contains("\"id\":1");
    }

    @Test
    @Order(11)
    @DisplayName("E2E: Should get all products")
    void shouldGetAllProducts() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/products"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Laptop\"");
    }

    @Test
    @Order(12)
    @DisplayName("E2E: Should handle CRUD operations on multiple entities")
    void shouldHandleCrudOperationsOnMultipleEntities() throws IOException, InterruptedException {
        // Create product
        String productBody = "{\"name\":\"Mouse\",\"price\":\"25\"}";
        HttpRequest createProduct = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/products"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(productBody))
            .build();
        httpClient.send(createProduct, HttpResponse.BodyHandlers.ofString());

        // Create user
        String userBody = "{\"name\":\"Alice\",\"email\":\"alice@example.com\"}";
        HttpRequest createUser = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(userBody))
            .build();
        httpClient.send(createUser, HttpResponse.BodyHandlers.ofString());

        // Verify both exist
        HttpRequest getProducts = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/products"))
            .GET()
            .build();
        HttpResponse<String> productsResponse = httpClient.send(getProducts, HttpResponse.BodyHandlers.ofString());

        HttpRequest getUsers = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .GET()
            .build();
        HttpResponse<String> usersResponse = httpClient.send(getUsers, HttpResponse.BodyHandlers.ofString());

        assertThat(productsResponse.body()).contains("\"name\":\"Mouse\"");
        assertThat(usersResponse.body()).contains("\"name\":\"Alice\"");
    }

    @Test
    @Order(13)
    @DisplayName("E2E: Should return 404 for non-existent route")
    void shouldReturn404ForNonExistentRoute() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/nonexistent"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    @Order(14)
    @DisplayName("E2E: Should serve index.html at root path")
    void shouldServeIndexHtmlAtRootPath() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
            .isPresent()
            .get().isEqualTo("text/html");
    }

    @Test
    @Order(15)
    @DisplayName("E2E: Should handle invalid JSON gracefully")
    void shouldHandleInvalidJsonGracefully() throws IOException, InterruptedException {
        String invalidJson = "{invalid json}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Should still process - might have null fields but won't crash
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
