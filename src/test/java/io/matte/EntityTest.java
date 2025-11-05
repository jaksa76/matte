package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Entity Unit Tests")
class EntityTest {

    static class TestEntity extends Entity {
        final Field<String> name = field("name", String.class);
        final Field<Integer> age = field("age", Integer.class);

        public TestEntity() {
            fields(name, age);
        }
    }

    private TestEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TestEntity();
    }

    @Test
    @DisplayName("Should have id field by default")
    void shouldHaveIdFieldByDefault() {
        assertThat(entity.id).isNotNull();
        assertThat(entity.id.fieldName()).isEqualTo("id");
        assertThat(entity.id.type()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("Should register id field in data map")
    void shouldRegisterIdFieldInDataMap() {
        assertThat(entity.data).containsKey("id");
        assertThat(entity.data.get("id")).isEqualTo(entity.id);
    }

    @Test
    @DisplayName("Should register custom fields in data map")
    void shouldRegisterCustomFieldsInDataMap() {
        assertThat(entity.data).containsKeys("name", "age");
        assertThat(entity.data.get("name")).isEqualTo(entity.name);
        assertThat(entity.data.get("age")).isEqualTo(entity.age);
    }

    @Test
    @DisplayName("Should have three fields total (id, name, age)")
    void shouldHaveThreeFieldsTotal() {
        assertThat(entity.data).hasSize(3);
    }

    @Test
    @DisplayName("Should allow setting and getting field values")
    void shouldAllowSettingAndGettingFieldValues() {
        entity.name.set("Alice");
        entity.age.set(30);
        entity.id.set(100L);

        assertThat(entity.name.get()).isEqualTo("Alice");
        assertThat(entity.age.get()).isEqualTo(30);
        assertThat(entity.id.get()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should create fields with correct types")
    void shouldCreateFieldsWithCorrectTypes() {
        assertThat(entity.name.type()).isEqualTo(String.class);
        assertThat(entity.age.type()).isEqualTo(Integer.class);
    }

    @Test
    @DisplayName("Should allow entity without custom fields")
    void shouldAllowEntityWithoutCustomFields() {
        Entity emptyEntity = new Entity();

        assertThat(emptyEntity.data).hasSize(1);
        assertThat(emptyEntity.data).containsKey("id");
    }

    @Test
    @DisplayName("Should support boolean fields")
    void shouldSupportBooleanFields() {
        class BooleanEntity extends Entity {
            final Field<Boolean> active = field("active", Boolean.class);

            public BooleanEntity() {
                fields(active);
            }
        }

        BooleanEntity boolEntity = new BooleanEntity();
        boolEntity.active.set(true);

        assertThat(boolEntity.active.get()).isTrue();
        assertThat(boolEntity.data.get("active")).isEqualTo(boolEntity.active);
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

        LongEntity longEntity = new LongEntity();
        longEntity.timestamp.set(1234567890L);

        assertThat(longEntity.timestamp.get()).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("Should allow multiple entities with same structure")
    void shouldAllowMultipleEntitiesWithSameStructure() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();

        entity1.name.set("Alice");
        entity2.name.set("Bob");

        assertThat(entity1.name.get()).isEqualTo("Alice");
        assertThat(entity2.name.get()).isEqualTo("Bob");
    }
}
