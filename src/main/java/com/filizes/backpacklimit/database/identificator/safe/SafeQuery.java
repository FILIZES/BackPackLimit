package com.filizes.backpacklimit.database.identificator.safe;

import com.filizes.backpacklimit.database.identificator.SQLIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public record SafeQuery(@NotNull String sql) {
    public static SafeQuery from(@NotNull String template, @NotNull SQLIdentifier... identifiers) {
        Object[] quotedIdentifiers = Stream.of(identifiers)
                .map(SQLIdentifier::quoted)
                .toArray();
        return new SafeQuery(template.formatted(quotedIdentifiers));
    }
}