package com.filizes.backpacklimit.database.factory.type;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public enum StorageType {
    MYSQL,
    SQLITE;

    public static StorageType fromString(@NotNull String type) {
        return Stream.of(values())
                .filter(storageType -> storageType.name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(SQLITE);
    }
}