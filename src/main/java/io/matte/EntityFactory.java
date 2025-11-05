package io.matte;

public interface EntityFactory<T extends Entity> {
    T create();
}
