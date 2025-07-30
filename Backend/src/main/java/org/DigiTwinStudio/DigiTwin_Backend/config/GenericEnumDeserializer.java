package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Arrays;

public class GenericEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {
    private final Class<T> enumType;

    public GenericEnumDeserializer(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getText();
        if (raw == null) return null;

        // Beispiel: "xs:string" → "STRING"
        String normalized = raw.trim()
                .replaceAll("^xs:", "")                // entferne XML-Präfixe
                .replaceAll("([a-z])([A-Z])", "$1_$2") // camelCase → snake_case
                .replaceAll("[\\-\\s]", "_")           // Leerzeichen und Bindestriche ersetzen
                .toUpperCase();                        // alles groß

        return Arrays.stream(enumType.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() ->
                        new IOException("Unknown value '" + raw + "' for enum " + enumType.getSimpleName() +
                                ". Allowed values: " + Arrays.toString(enumType.getEnumConstants())));
    }


}
