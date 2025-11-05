package io.matte;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Repository<T extends Entity> {
    private final String name;
    private final Map<Long, T> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Repository(String name) {
        this.name = name;
    }

    public T save(T entity) {
        Long id = entity.id.get();
        if (id == null) {
            id = idGenerator.getAndIncrement();
            entity.id.set(id);
        }
        store.put(id, entity);
        return entity;
    }

    public T findById(Long id) {
        return store.get(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public int count() {
        return store.size();
    }
}
