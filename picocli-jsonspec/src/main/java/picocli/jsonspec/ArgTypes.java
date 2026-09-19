package picocli.jsonspec;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps between the short type names used in a JSON spec or the DSL (e.g. {@code "boolean"},
 * {@code "File"}) and the actual {@link Class} picocli should use for an option or positional
 * parameter. Shared by {@link CommandSpecJson} and the DSL parser so both accept and produce
 * the same vocabulary.
 * <p>Scope is deliberately limited to scalar types for now; array/Collection-typed arguments
 * are not yet supported.</p>
 */
final class ArgTypes {

    private ArgTypes() {}

    private static final Map<String, Class<?>> TYPES_BY_NAME = new LinkedHashMap<String, Class<?>>();
    static {
        TYPES_BY_NAME.put("String", String.class);
        TYPES_BY_NAME.put("boolean", boolean.class);
        TYPES_BY_NAME.put("int", int.class);
        TYPES_BY_NAME.put("long", long.class);
        TYPES_BY_NAME.put("double", double.class);
        TYPES_BY_NAME.put("File", File.class);
    }
    private static final Map<Class<?>, String> NAMES_BY_TYPE = new LinkedHashMap<Class<?>, String>();
    static {
        for (Map.Entry<String, Class<?>> entry : TYPES_BY_NAME.entrySet()) {
            NAMES_BY_TYPE.put(entry.getValue(), entry.getKey());
        }
    }

    static Class<?> toClass(String typeName) {
        Class<?> type = TYPES_BY_NAME.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type \"" + typeName + "\": supported types are " + TYPES_BY_NAME.keySet());
        }
        return type;
    }

    static String toName(Class<?> type) {
        String name = NAMES_BY_TYPE.get(type);
        return name != null ? name : type.getSimpleName();
    }
}
