package io.matte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Repository Unit Tests")
class RepositoryTest {

    static class TestEntity extends Entity {
        final Field<String> name = field("name", String.class);

        public TestEntity() {
            fields(name);
        }
    }

    private Repository<TestEntity> repository;

    @BeforeEach
    void setUp() {
        repository = new Repository<>("test-entities");
    }

    @Test
    @DisplayName("Should save entity without id and generate new id")
    void shouldSaveEntityWithoutIdAndGenerateNewId() {
        TestEntity entity = new TestEntity();
        entity.name.set("Test");

        TestEntity saved = repository.save(entity);

        assertThat(saved.id.get()).isNotNull();
        assertThat(saved.id.get()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should auto-increment id for multiple entities")
    void shouldAutoIncrementIdForMultipleEntities() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();
        TestEntity entity3 = new TestEntity();

        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        assertThat(entity1.id.get()).isEqualTo(1L);
        assertThat(entity2.id.get()).isEqualTo(2L);
        assertThat(entity3.id.get()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should save entity with existing id without changing it")
    void shouldSaveEntityWithExistingIdWithoutChangingIt() {
        TestEntity entity = new TestEntity();
        entity.id.set(100L);
        entity.name.set("Test");

        TestEntity saved = repository.save(entity);

        assertThat(saved.id.get()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should find entity by id")
    void shouldFindEntityById() {
        TestEntity entity = new TestEntity();
        entity.name.set("Alice");
        repository.save(entity);

        TestEntity found = repository.findById(entity.id.get());

        assertThat(found).isNotNull();
        assertThat(found.name.get()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("Should return null when entity not found by id")
    void shouldReturnNullWhenEntityNotFoundById() {
        TestEntity found = repository.findById(999L);

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should find all entities")
    void shouldFindAllEntities() {
        TestEntity entity1 = new TestEntity();
        entity1.name.set("Alice");
        TestEntity entity2 = new TestEntity();
        entity2.name.set("Bob");

        repository.save(entity1);
        repository.save(entity2);

        List<TestEntity> all = repository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(e -> e.name.get()).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @DisplayName("Should return empty list when no entities exist")
    void shouldReturnEmptyListWhenNoEntitiesExist() {
        List<TestEntity> all = repository.findAll();

        assertThat(all).isEmpty();
    }

    @Test
    @DisplayName("Should delete entity by id")
    void shouldDeleteEntityById() {
        TestEntity entity = new TestEntity();
        repository.save(entity);
        Long id = entity.id.get();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isNull();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should handle deleting non-existent entity gracefully")
    void shouldHandleDeletingNonExistentEntityGracefully() {
        assertThatCode(() -> repository.deleteById(999L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should return correct count of entities")
    void shouldReturnCorrectCountOfEntities() {
        assertThat(repository.count()).isEqualTo(0);

        repository.save(new TestEntity());
        assertThat(repository.count()).isEqualTo(1);

        repository.save(new TestEntity());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update existing entity when saved again")
    void shouldUpdateExistingEntityWhenSavedAgain() {
        TestEntity entity = new TestEntity();
        entity.name.set("Original");
        repository.save(entity);
        Long id = entity.id.get();

        entity.name.set("Updated");
        repository.save(entity);

        TestEntity found = repository.findById(id);
        assertThat(found.name.get()).isEqualTo("Updated");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle multiple repositories independently")
    void shouldHandleMultipleRepositoriesIndependently() {
        Repository<TestEntity> repo1 = new Repository<>("repo1");
        Repository<TestEntity> repo2 = new Repository<>("repo2");

        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();

        repo1.save(entity1);
        repo2.save(entity2);

        assertThat(repo1.count()).isEqualTo(1);
        assertThat(repo2.count()).isEqualTo(1);
        assertThat(entity1.id.get()).isEqualTo(1L);
        assertThat(entity2.id.get()).isEqualTo(1L); // Independent ID generation
    }

    @Test
    @DisplayName("Should maintain entity references correctly")
    void shouldMaintainEntityReferencesCorrectly() {
        TestEntity entity = new TestEntity();
        entity.name.set("Test");
        repository.save(entity);

        entity.name.set("Modified");

        TestEntity found = repository.findById(entity.id.get());
        assertThat(found.name.get()).isEqualTo("Modified");
        assertThat(found).isSameAs(entity);
    }

    @Test
    @DisplayName("Should handle saving same entity multiple times")
    void shouldHandleSavingSameEntityMultipleTimes() {
        TestEntity entity = new TestEntity();
        entity.name.set("First");

        repository.save(entity);
        Long id1 = entity.id.get();

        repository.save(entity);
        Long id2 = entity.id.get();

        assertThat(id1).isEqualTo(id2);
        assertThat(repository.count()).isEqualTo(1);
    }
}
