package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EntityController Unit Tests")
class EntityControllerTest {

    static class TestEntity extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> age = field("age", Integer.class);

        public TestEntity() {
            fields(name, age);
        }
    }

    private Repository<TestEntity> repository;
    private EntityController<TestEntity> controller;
    private EntityFactory<TestEntity> factory;

    @BeforeEach
    void setUp() {
        repository = new Repository<>("users");
        factory = TestEntity::new;
        controller = new EntityController<>(repository, "users", factory);
    }

    @Test
    @DisplayName("Should handle GET all entities request")
    void shouldHandleGetAllEntitiesRequest() {
        TestEntity entity1 = new TestEntity();
        entity1.name.set("Alice");
        repository.save(entity1);

        TestEntity entity2 = new TestEntity();
        entity2.name.set("Bob");
        repository.save(entity2);

        String response = controller.handleRequest("GET", "/api/users", "");

        assertThat(response).contains("\"name\":\"Alice\"");
        assertThat(response).contains("\"name\":\"Bob\"");
        assertThat(response).startsWith("[");
        assertThat(response).endsWith("]");
    }

    @Test
    @DisplayName("Should return empty array when no entities exist")
    void shouldReturnEmptyArrayWhenNoEntitiesExist() {
        String response = controller.handleRequest("GET", "/api/users", "");

        assertThat(response).isEqualTo("[]");
    }

    @Test
    @DisplayName("Should handle GET entity by id request")
    void shouldHandleGetEntityByIdRequest() {
        TestEntity entity = new TestEntity();
        entity.name.set("Alice");
        entity.age.set(30);
        repository.save(entity);
        Long id = entity.id.get();

        String response = controller.handleRequest("GET", "/api/users/" + id, "");

        assertThat(response).contains("\"id\":" + id);
        assertThat(response).contains("\"name\":\"Alice\"");
        assertThat(response).contains("\"age\":30");
    }

    @Test
    @DisplayName("Should return error when entity not found by id")
    void shouldReturnErrorWhenEntityNotFoundById() {
        String response = controller.handleRequest("GET", "/api/users/999", "");

        assertThat(response).contains("\"error\"");
        assertThat(response).contains("not found");
        assertThat(response).contains("\"status\":404");
    }

    @Test
    @DisplayName("Should handle POST create entity request")
    void shouldHandlePostCreateEntityRequest() {
        String requestBody = "{\"name\":\"Charlie\",\"age\":\"25\"}";

        String response = controller.handleRequest("POST", "/api/users", requestBody);

        assertThat(response).contains("\"name\":\"Charlie\"");
        assertThat(response).contains("\"age\":25");
        assertThat(response).contains("\"id\":1");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle PUT update entity request")
    void shouldHandlePutUpdateEntityRequest() {
        TestEntity entity = new TestEntity();
        entity.name.set("Original");
        entity.age.set(20);
        repository.save(entity);
        Long id = entity.id.get();

        String requestBody = "{\"name\":\"Updated\",\"age\":\"25\"}";
        String response = controller.handleRequest("PUT", "/api/users/" + id, requestBody);

        assertThat(response).contains("\"name\":\"Updated\"");
        assertThat(response).contains("\"age\":25");
        
        TestEntity updated = repository.findById(id);
        assertThat(updated.name.get()).isEqualTo("Updated");
        assertThat(updated.age.get()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should return error when updating non-existent entity")
    void shouldReturnErrorWhenUpdatingNonExistentEntity() {
        String requestBody = "{\"name\":\"Test\"}";
        String response = controller.handleRequest("PUT", "/api/users/999", requestBody);

        assertThat(response).contains("\"error\"");
        assertThat(response).contains("not found");
        assertThat(response).contains("\"status\":404");
    }

    @Test
    @DisplayName("Should handle DELETE entity request")
    void shouldHandleDeleteEntityRequest() {
        TestEntity entity = new TestEntity();
        entity.name.set("ToDelete");
        repository.save(entity);
        Long id = entity.id.get();

        String response = controller.handleRequest("DELETE", "/api/users/" + id, "");

        assertThat(response).contains("\"message\"");
        assertThat(response).contains("deleted successfully");
        assertThat(repository.findById(id)).isNull();
    }

    @Test
    @DisplayName("Should return error when deleting non-existent entity")
    void shouldReturnErrorWhenDeletingNonExistentEntity() {
        String response = controller.handleRequest("DELETE", "/api/users/999", "");

        assertThat(response).contains("\"error\"");
        assertThat(response).contains("not found");
        assertThat(response).contains("\"status\":404");
    }

    @Test
    @DisplayName("Should return error for invalid id format")
    void shouldReturnErrorForInvalidIdFormat() {
        String response = controller.handleRequest("GET", "/api/users/invalid", "");

        assertThat(response).contains("\"error\"");
        assertThat(response).contains("Invalid ID format");
        assertThat(response).contains("\"status\":400");
    }

    @Test
    @DisplayName("Should return error for not found route")
    void shouldReturnErrorForNotFoundRoute() {
        String response = controller.handleRequest("PATCH", "/api/users", "");

        assertThat(response).contains("\"error\"");
        assertThat(response).contains("Not Found");
        assertThat(response).contains("\"status\":404");
    }

    @Test
    @DisplayName("Should skip id field when creating entity from JSON")
    void shouldSkipIdFieldWhenCreatingEntityFromJson() {
        String requestBody = "{\"id\":\"999\",\"name\":\"Test\"}";

        String response = controller.handleRequest("POST", "/api/users", requestBody);

        assertThat(response).contains("\"id\":1"); // Should be auto-generated, not 999
        assertThat(response).contains("\"name\":\"Test\"");
    }

    @Test
    @DisplayName("Should handle JSON with missing fields gracefully")
    void shouldHandleJsonWithMissingFieldsGracefully() {
        String requestBody = "{\"name\":\"OnlyName\"}";

        String response = controller.handleRequest("POST", "/api/users", requestBody);

        assertThat(response).contains("\"name\":\"OnlyName\"");
        assertThat(response).contains("\"age\":null");
    }

    @Test
    @DisplayName("Should handle invalid integer values gracefully")
    void shouldHandleInvalidIntegerValuesGracefully() {
        String requestBody = "{\"name\":\"Test\",\"age\":\"notanumber\"}";

        String response = controller.handleRequest("POST", "/api/users", requestBody);

        assertThat(response).contains("\"name\":\"Test\"");
        assertThat(response).contains("\"age\":null"); // Should skip invalid value
    }

    @Test
    @DisplayName("Should parse string fields correctly")
    void shouldParseStringFieldsCorrectly() {
        String requestBody = "{\"name\":\"John Doe\"}";

        controller.handleRequest("POST", "/api/users", requestBody);

        TestEntity created = repository.findById(1L);
        assertThat(created.name.get()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should parse integer fields correctly")
    void shouldParseIntegerFieldsCorrectly() {
        String requestBody = "{\"age\":\"42\"}";

        controller.handleRequest("POST", "/api/users", requestBody);

        TestEntity created = repository.findById(1L);
        assertThat(created.age.get()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should support boolean fields")
    void shouldSupportBooleanFields() {
        class BoolEntity extends Entity {
            final Field<Boolean> active = field("active", Boolean.class);

            public BoolEntity() {
                fields(active);
            }
        }

        Repository<BoolEntity> boolRepo = new Repository<>("bools");
        EntityController<BoolEntity> boolController = 
            new EntityController<>(boolRepo, "bools", BoolEntity::new);

        String requestBody = "{\"active\":\"true\"}";
        String response = boolController.handleRequest("POST", "/api/bools", requestBody);

        assertThat(response).contains("\"active\":true");
    }

    @Test
    @DisplayName("Should support long fields")
    void shouldSupportLongFields() {
        class LongEntity extends Entity {
            final Field<Long> timestamp = field("timestamp", Long.class);

            public LongEntity() {
                fields(timestamp);
            }
        }

        Repository<LongEntity> longRepo = new Repository<>("longs");
        EntityController<LongEntity> longController = 
            new EntityController<>(longRepo, "longs", LongEntity::new);

        String requestBody = "{\"timestamp\":\"1234567890\"}";
        String response = longController.handleRequest("POST", "/api/longs", requestBody);

        assertThat(response).contains("\"timestamp\":1234567890");
    }

    @Test
    @DisplayName("Should capitalize resource name in error messages")
    void shouldCapitalizeResourceNameInErrorMessages() {
        String response = controller.handleRequest("GET", "/api/users/999", "");

        assertThat(response).contains("Users not found");
    }
}
