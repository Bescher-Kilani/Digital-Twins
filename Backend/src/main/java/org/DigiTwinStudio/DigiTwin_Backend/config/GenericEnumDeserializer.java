package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Arrays;

/**
 * Generic Jackson deserializer for Enum types.
 * <p>
 * This deserializer enables robust parsing of enums from JSON by accepting
 * various representations (including different cases, underscores, dashes,
 * spaces, and even XML prefixes like "xs:"). The raw JSON value is normalized
 * and then matched against the enum constants in a case-insensitive way.
 * </p>
 *
 * <p>
 * Example: The JSON value <code>"xs:string"</code> or <code>"String"</code>
 * will both be mapped to <code>STRING</code> in the corresponding enum.
 * </p>
 *
 * @param <T> the specific Enum type
 */
public class GenericEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {
    private final Class<T> enumType;

    /**
     * Constructs a generic enum deserializer for the specified enum type.
     *
     * @param enumType the enum class to deserialize to
     */
    public GenericEnumDeserializer(Class<T> enumType) {
        this.enumType = enumType;
    }

    /**
     * Deserializes the JSON value to the corresponding enum constant,
     * accepting a wide range of formats and normalizing the input.
     *
     * @param p     the JSON parser
     * @param ctxt  the deserialization context
     * @return the matching enum constant, or throws an exception if not found
     * @throws IOException if the value cannot be mapped to an enum constant
     */
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getText();
        if (raw == null) return null;

        // Normalize the raw value to match enum constants
        String normalized = raw.trim()
                .replaceAll("^xs:", "")                // remove XML prefixes
                .replaceAll("([a-z])([A-Z])", "$1_$2") // camelCase to snake_case
                .replaceAll("[\\-\\s]", "_")           // replace dashes and spaces with underscores
                .toUpperCase();                        // uppercase

        return Arrays.stream(enumType.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() ->
                        new IOException("Unknown value '" + raw + "' for enum " + enumType.getSimpleName() +
                                ". Allowed values: " + Arrays.toString(enumType.getEnumConstants())));
    }
}
