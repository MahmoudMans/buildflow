package com.buildflow.audit.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks the value of password-like and token-like fields before audit values
 * leave the server.
 *
 * Audit values are the text form of whatever a service returned, so they
 * look like {@code TaskView[id=1, title=Pour slab]} or like JSON. A field is
 * masked when its name contains one of the words below. A few harmless fields
 * may be masked too; that is the safe direction to be wrong in.
 */
public final class AuditValueRedactor {

    public static final String MASK = "[redacted]";

    private static final String SENSITIVE =
        "(?:password|passwd|passphrase|secret|token|apikey|api_key"
            + "|authorization|credential|hash)";

    /**
     * name=value, where the value ends at a comma or a closing bracket. A
     * value that is already masked is matched whole, so masking twice does
     * not change the result.
     */
    private static final Pattern RECORD_STYLE =
        Pattern.compile(
            "(\\b\\w*" + SENSITIVE + "\\w*\\s*=\\s*)"
                + "(\\[redacted\\]|[^,\\]\\)\\}]*)",
            Pattern.CASE_INSENSITIVE
        );

    /** "name": "value" or "name": 123 */
    private static final Pattern JSON_STYLE =
        Pattern.compile(
            "(\"\\w*" + SENSITIVE + "\\w*\"\\s*:\\s*)"
                + "(\"(?:[^\"\\\\]|\\\\.)*\"|[^,}\\]\\s]+)",
            Pattern.CASE_INSENSITIVE
        );

    private AuditValueRedactor() {
    }

    public static String redact(String value) {

        if (value == null || value.isEmpty()) {
            return value;
        }

        String json =
            JSON_STYLE
                .matcher(value)
                .replaceAll(
                    match ->
                        Matcher.quoteReplacement(
                            match.group(1) + "\"" + MASK + "\""
                        )
                );

        return RECORD_STYLE
            .matcher(json)
            .replaceAll(
                match ->
                    Matcher.quoteReplacement(
                        match.group(1) + MASK
                    )
            );
    }
}
