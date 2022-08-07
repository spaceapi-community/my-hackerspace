/*
 * Copyright (C) 2020-2022 Danilo Bargen (dbrgn)
 * Licensed under GNU's GPL 3, see README
 */
package io.spaceapi.community.myhackerspace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Utils {
    /**
     * Join the strings using the specified separator.
     */
    public static @Nullable
    String joinStrings(@NonNull String separator, String... strings) {
        final StringBuilder builder = new StringBuilder();
        boolean empty = true;
        for (String string : strings) {
            if (string != null) {
                if (empty) {
                    builder.append(string);
                    empty = false;
                } else {
                    builder.append(separator).append(string);
                }
            }
        }
        return empty ? null : builder.toString();
    }
}
