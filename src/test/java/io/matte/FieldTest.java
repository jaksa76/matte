package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Field Unit Tests")
class FieldTest {

    @Test
    @DisplayName("Should create field with name and type")
    void shouldCreateFieldWithNameAndType() {
        Field<String> field = new Field<>("name", String.class);
        
        assertThat(field.fieldName()).isEqualTo("name");
        assertThat(field.type()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("Should initially have null value")
    void shouldInitiallyHaveNullValue() {
        Field<String> field = new Field<>("name", String.class);
        
        assertThat(field.get()).isNull();
    }

    @Test
    @DisplayName("Should set and get string value")
    void shouldSetAndGetStringValue() {
        Field<String> field = new Field<>("name", String.class);
        field.set("John Doe");
        
        assertThat(field.get()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should set and get integer value")
    void shouldSetAndGetIntegerValue() {
        Field<Integer> field = new Field<>("age", Integer.class);
        field.set(42);
        
        assertThat(field.get()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should set and get long value")
    void shouldSetAndGetLongValue() {
        Field<Long> field = new Field<>("id", Long.class);
        field.set(123456L);
        
        assertThat(field.get()).isEqualTo(123456L);
    }

    @Test
    @DisplayName("Should set and get boolean value")
    void shouldSetAndGetBooleanValue() {
        Field<Boolean> field = new Field<>("active", Boolean.class);
        field.set(true);
        
        assertThat(field.get()).isTrue();
    }

    @Test
    @DisplayName("Should update value when set multiple times")
    void shouldUpdateValueWhenSetMultipleTimes() {
        Field<String> field = new Field<>("name", String.class);
        field.set("First");
        field.set("Second");
        
        assertThat(field.get()).isEqualTo("Second");
    }

    @Test
    @DisplayName("Should accept null value after setting non-null")
    void shouldAcceptNullValueAfterSettingNonNull() {
        Field<String> field = new Field<>("name", String.class);
        field.set("John");
        field.set(null);
        
        assertThat(field.get()).isNull();
    }

    @Test
    @DisplayName("Should work with different field name types")
    void shouldWorkWithDifferentFieldNameTypes() {
        Field<String> stringField = new Field<>("fieldName", String.class);
        Field<String> intField = new Field<>(123, String.class);
        
        assertThat(stringField.fieldName()).isEqualTo("fieldName");
        assertThat(intField.fieldName()).isEqualTo(123);
    }
}
