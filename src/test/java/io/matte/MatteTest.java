package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Matte Integration Tests")
class MatteTest {

    static class TestEntity extends Entity {
        final Field<String> name = field("name", String.class);

        public TestEntity() {
            fields(name);
        }
    }

    static class Product extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> price = field("price", Integer.class);

        public Product() {
            fields(name, price);
        }
    }

    private Matte app;

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @DisplayName("Should create Matte instance with default port")
    void shouldCreateMatteInstanceWithDefaultPort() {
        app = new Matte();

        assertThat(app).isNotNull();
    }

    @Test
    @DisplayName("Should create Matte instance with custom port")
    void shouldCreateMatteInstanceWithCustomPort() {
        app = new Matte(9090);

        assertThat(app).isNotNull();
    }

    @Test
    @DisplayName("Should register entity and return repository")
    void shouldRegisterEntityAndReturnRepository() {
        app = new Matte();
        app.register("users", TestEntity::new);

        Repository<TestEntity> repository = app.getRepository("users");

        assertThat(repository).isNotNull();
    }

    @Test
    @DisplayName("Should register entity and return controller")
    void shouldRegisterEntityAndReturnController() {
        app = new Matte();
        app.register("users", TestEntity::new);

        EntityController<TestEntity> controller = app.getController("users");

        assertThat(controller).isNotNull();
    }

    @Test
    @DisplayName("Should support method chaining for registration")
    void shouldSupportMethodChainingForRegistration() {
        app = new Matte()
            .register("users", TestEntity::new)
            .register("products", Product::new);

        assertThat(app.getRepository("users")).isNotNull();
        assertThat(app.getRepository("products")).isNotNull();
    }

    @Test
    @DisplayName("Should register multiple entities independently")
    void shouldRegisterMultipleEntitiesIndependently() {
        app = new Matte();
        app.register("users", TestEntity::new);
        app.register("products", Product::new);

        Repository<TestEntity> userRepo = app.getRepository("users");
        Repository<Product> productRepo = app.getRepository("products");

        assertThat(userRepo).isNotNull();
        assertThat(productRepo).isNotNull();
        assertThat(userRepo).isNotSameAs(productRepo);
    }

    @Test
    @DisplayName("Should allow adding data to repository before starting")
    void shouldAllowAddingDataToRepositoryBeforeStarting() {
        app = new Matte();
        app.register("users", TestEntity::new);

        Repository<TestEntity> repository = app.getRepository("users");
        TestEntity entity = new TestEntity();
        entity.name.set("Alice");
        repository.save(entity);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(1L).name.get()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("Should start server successfully with registered entities")
    void shouldStartServerSuccessfullyWithRegisteredEntities() throws IOException {
        app = new Matte(8081);
        app.register("users", TestEntity::new);

        assertThatCode(() -> app.start()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle starting server without entities")
    void shouldHandleStartingServerWithoutEntities() throws IOException {
        app = new Matte(8082);

        assertThatCode(() -> app.start()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should stop server without errors")
    void shouldStopServerWithoutErrors() throws IOException {
        app = new Matte(8083);
        app.register("users", TestEntity::new);
        app.start();

        assertThatCode(() -> app.stop()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle stop without starting")
    void shouldHandleStopWithoutStarting() {
        app = new Matte();

        assertThatCode(() -> app.stop()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should return null for non-existent repository")
    void shouldReturnNullForNonExistentRepository() {
        app = new Matte();

        Repository<TestEntity> repository = app.getRepository("nonexistent");

        assertThat(repository).isNull();
    }

    @Test
    @DisplayName("Should return null for non-existent controller")
    void shouldReturnNullForNonExistentController() {
        app = new Matte();

        EntityController<TestEntity> controller = app.getController("nonexistent");

        assertThat(controller).isNull();
    }

    @Test
    @DisplayName("Should support method chaining with start")
    void shouldSupportMethodChainingWithStart() throws IOException {
        assertThatCode(() -> {
            app = new Matte(8084)
                .register("users", TestEntity::new)
                .start();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should maintain entity data after server start")
    void shouldMaintainEntityDataAfterServerStart() throws IOException {
        app = new Matte(8085);
        app.register("users", TestEntity::new);

        Repository<TestEntity> repository = app.getRepository("users");
        TestEntity entity = new TestEntity();
        entity.name.set("Bob");
        repository.save(entity);

        app.start();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(1L).name.get()).isEqualTo("Bob");
    }
}
