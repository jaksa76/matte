package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JsonSerializer Unit Tests")
class JsonSerializerTest {

    static class TestEntity extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> age = field("age", Integer.class);

        public TestEntity() {
            fields(name, age);
        }
    }

    static class CompleteEntity extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> count = field("count", Integer.class);
        final Field<Long> timestamp = field("timestamp", Long.class);
        final Field<Boolean> active = field("active", Boolean.class);

        public CompleteEntity() {
            fields(name, count, timestamp, active);
        }
    }

    private TestEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TestEntity();
    }

    @Test
    @DisplayName("Should serialize entity with all fields null")
    void shouldSerializeEntityWithAllFieldsNull() {
        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"id\":null");
        assertThat(json).contains("\"name\":null");
        assertThat(json).contains("\"age\":null");
    }

    @Test
    @DisplayName("Should serialize entity with string values")
    void shouldSerializeEntityWithStringValues() {
        entity.name.set("John Doe");

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"name\":\"John Doe\"");
    }

    @Test
    @DisplayName("Should serialize entity with integer values")
    void shouldSerializeEntityWithIntegerValues() {
        entity.age.set(42);

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"age\":42");
    }

    @Test
    @DisplayName("Should serialize entity with id")
    void shouldSerializeEntityWithId() {
        entity.id.set(123L);

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"id\":123");
    }

    @Test
    @DisplayName("Should serialize complete entity with all field types")
    void shouldSerializeCompleteEntityWithAllFieldTypes() {
        CompleteEntity complete = new CompleteEntity();
        complete.id.set(1L);
        complete.name.set("Test");
        complete.count.set(100);
        complete.timestamp.set(1234567890L);
        complete.active.set(true);

        String json = JsonSerializer.toJson(complete);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"Test\"");
        assertThat(json).contains("\"count\":100");
        assertThat(json).contains("\"timestamp\":1234567890");
        assertThat(json).contains("\"active\":true");
    }

    @Test
    @DisplayName("Should escape quotes in string values")
    void shouldEscapeQuotesInStringValues() {
        entity.name.set("John \"The Boss\" Doe");

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"name\":\"John \\\"The Boss\\\" Doe\"");
    }

    @Test
    @DisplayName("Should produce valid JSON format")
    void shouldProduceValidJsonFormat() {
        entity.id.set(1L);
        entity.name.set("Alice");
        entity.age.set(30);

        String json = JsonSerializer.toJson(entity);

        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
        assertThat(json).contains(",");
    }

    @Test
    @DisplayName("Should serialize boolean true value")
    void shouldSerializeBooleanTrueValue() {
        CompleteEntity complete = new CompleteEntity();
        complete.active.set(true);

        String json = JsonSerializer.toJson(complete);

        assertThat(json).contains("\"active\":true");
    }

    @Test
    @DisplayName("Should serialize boolean false value")
    void shouldSerializeBooleanFalseValue() {
        CompleteEntity complete = new CompleteEntity();
        complete.active.set(false);

        String json = JsonSerializer.toJson(complete);

        assertThat(json).contains("\"active\":false");
    }

    @Test
    @DisplayName("Should serialize empty entity with only id")
    void shouldSerializeEmptyEntityWithOnlyId() {
        Entity emptyEntity = new Entity();
        emptyEntity.id.set(999L);

        String json = JsonSerializer.toJson(emptyEntity);

        assertThat(json).isEqualTo("{\"id\":999}");
    }

    @Test
    @DisplayName("Should handle zero values correctly")
    void shouldHandleZeroValuesCorrectly() {
        entity.age.set(0);
        entity.id.set(0L);

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"age\":0");
        assertThat(json).contains("\"id\":0");
    }

    @Test
    @DisplayName("Should handle empty string values")
    void shouldHandleEmptyStringValues() {
        entity.name.set("");

        String json = JsonSerializer.toJson(entity);

        assertThat(json).contains("\"name\":\"\"");
    }

    @Test
    @DisplayName("Should serialize long values correctly")
    void shouldSerializeLongValuesCorrectly() {
        CompleteEntity complete = new CompleteEntity();
        complete.timestamp.set(9223372036854775807L); // Long.MAX_VALUE

        String json = JsonSerializer.toJson(complete);

        assertThat(json).contains("\"timestamp\":9223372036854775807");
    }
}
