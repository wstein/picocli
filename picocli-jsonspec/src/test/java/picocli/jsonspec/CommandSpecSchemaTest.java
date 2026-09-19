package picocli.jsonspec;

import org.junit.Test;
import picocli.jsonspec.json.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards against drift between the published JSON Schema
 * ({@code /picocli/jsonspec/command-spec.schema.json} on the classpath) and what
 * {@link CommandSpecJson}/{@link ArgTypes} actually accept, by checking the schema's declared
 * {@code type} enum and {@code arity} pattern against the real vocabulary and against the
 * checked-in {@code flix.json} fixture. This is a structural smoke test, not a full JSON
 * Schema validator (the module has no JSON Schema library dependency, by design).
 */
public class CommandSpecSchemaTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readClasspathJson(String resourcePath) {
        InputStream in = CommandSpecSchemaTest.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) { out.write(buffer, 0, read); }
            return (Map<String, Object>) Json.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
        } finally {
            try { in.close(); } catch (IOException ignored) { /* best effort */ }
        }
    }

    private static Map<String, Object> schema() {
        return readClasspathJson("/picocli/jsonspec/command-spec.schema.json");
    }

    @Test
    public void schemaIsWellFormedJson() {
        Map<String, Object> schema = schema();
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.get("$schema"));
        assertTrue(schema.containsKey("$defs"));
    }

    /**
     * The checked-in schema's {@code $id} is the *unversioned* "latest" URL. The release
     * workflow (.github/workflows/release.yml) derives each release's pinned copy by a plain
     * text substitution of this exact literal into
     * {@code .../jsonspec/schema/<version>/command-spec.schema.json} -- if this URL ever
     * changes, that substitution must be updated too.
     */
    @Test
    public void schemaIdIsTheUnversionedLatestUrl() {
        assertEquals("https://wstein.github.io/picocli/jsonspec/schema/command-spec.schema.json", schema().get("$id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void schemaTypeEnumMatchesArgTypes() {
        Map<String, Object> schema = schema();
        Map<String, Object> defs = (Map<String, Object>) schema.get("$defs");
        Map<String, Object> typeDef = (Map<String, Object>) defs.get("type");
        List<Object> enumValues = (List<Object>) typeDef.get("enum");

        assertEquals(ArgTypes.names(), new HashSet<Object>(enumValues));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void everyFixtureTypeValueIsInTheSchemaEnum() {
        Map<String, Object> schema = schema();
        Map<String, Object> defs = (Map<String, Object>) schema.get("$defs");
        Set<Object> allowedTypes = new HashSet<Object>((List<Object>) ((Map<String, Object>) defs.get("type")).get("enum"));

        Map<String, Object> fixture = readClasspathJson("/picocli/jsonspec/fixtures/flix.json");
        Set<Object> typesInFixture = new HashSet<Object>();
        collectValuesOfKey(fixture, "type", typesInFixture);

        assertTrue("expected at least one \"type\" value in the fixture", !typesInFixture.isEmpty());
        assertTrue("fixture uses a type not in the schema enum: " + typesInFixture, allowedTypes.containsAll(typesInFixture));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void everyFixtureArityValueMatchesTheSchemaPattern() {
        Map<String, Object> schema = schema();
        Map<String, Object> defs = (Map<String, Object>) schema.get("$defs");
        Pattern arityPattern = Pattern.compile((String) ((Map<String, Object>) defs.get("arity")).get("pattern"));

        Map<String, Object> fixture = readClasspathJson("/picocli/jsonspec/fixtures/flix.json");
        Set<Object> aritiesInFixture = new HashSet<Object>();
        collectValuesOfKey(fixture, "arity", aritiesInFixture);

        assertTrue("expected at least one \"arity\" value in the fixture", !aritiesInFixture.isEmpty());
        for (Object arity : aritiesInFixture) {
            assertTrue("arity \"" + arity + "\" does not match schema pattern", arityPattern.matcher((String) arity).matches());
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectValuesOfKey(Object node, String key, Set<Object> result) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            if (map.containsKey(key)) { result.add(map.get(key)); }
            for (Object value : map.values()) { collectValuesOfKey(value, key, result); }
        } else if (node instanceof List) {
            for (Object item : (List<Object>) node) { collectValuesOfKey(item, key, result); }
        }
    }
}
