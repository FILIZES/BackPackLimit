package com.filizes.backpacklimit.database.identificator;

import com.filizes.backpacklimit.utils.ValidationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record SQLIdentifier(@NotNull String quoted) {

    public static SQLIdentifier of(@NotNull String identifier, @NotNull Function<String, String> quoter) {
        if (!ValidationUtils.isValidTableName(identifier)) {
            throw new IllegalArgumentException("Недопустимый SQL-идентификатор: " + identifier);
        }
        return new SQLIdentifier(quoter.apply(identifier));
    }
}