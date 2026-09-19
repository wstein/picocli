package picocli.jsonspec;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.jsonspec.json.Json;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a picocli {@link CommandSpec} from JSON, and writes a {@link CommandSpec} to JSON.
 * <p>This lets a CLI be defined without an annotated Java class: for example, to give a
 * beautiful, help-and-completion-enabled picocli front end to a third-party tool whose own
 * CLI is not human-friendly, describe that tool's commands/options/positional parameters as
 * JSON (or generate that JSON from the {@linkplain picocli.jsonspec.dsl lightweight DSL}) and
 * build a {@code CommandSpec} from it with {@link #read(String)}.</p>
 * <p>{@link #write(CommandSpec)} does the reverse for <em>any</em> {@code CommandSpec},
 * including ones built from annotated classes, e.g. for documentation or tooling that wants a
 * machine-readable description of a command's options.</p>
 */
public final class CommandSpecJson {

    private CommandSpecJson() {}

    /** Builds a new {@link CommandSpec} (with any nested subcommands) from the given JSON text. */
    @SuppressWarnings("unchecked")
    public static CommandSpec read(String json) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object describing a command, got: " + json);
        }
        return readCommand((Map<String, Object>) parsed);
    }

    /** Serializes the given {@link CommandSpec} (with any nested subcommands) to JSON text. */
    public static String write(CommandSpec spec) {
        return Json.write(writeCommand(spec));
    }

    // ---- reading: JSON -> CommandSpec ----

    @SuppressWarnings("unchecked")
    private static CommandSpec readCommand(Map<String, Object> json) {
        CommandSpec spec = CommandSpec.create();
        String name = (String) json.get("name");
        if (name != null) {
            spec.name(name);
        }
        String[] description = readStringArray(json.get("description"));
        if (description != null) {
            spec.usageMessage().description(description);
        }

        for (Object option : listOrEmpty(json.get("options"))) {
            spec.addOption(readOption((Map<String, Object>) option));
        }
        for (Object positional : listOrEmpty(json.get("positionalParams"))) {
            spec.addPositional(readPositional((Map<String, Object>) positional));
        }
        for (Object subcommand : listOrEmpty(json.get("subcommands"))) {
            Map<String, Object> subJson = (Map<String, Object>) subcommand;
            CommandSpec subSpec = readCommand(subJson);
            spec.addSubcommand(subSpec.name(), subSpec);
        }
        return spec;
    }

    @SuppressWarnings("unchecked")
    private static OptionSpec readOption(Map<String, Object> json) {
        List<Object> namesJson = (List<Object>) json.get("names");
        if (namesJson == null || namesJson.isEmpty()) {
            throw new IllegalArgumentException("Option is missing required \"names\" array: " + json);
        }
        String[] names = new String[namesJson.size()];
        for (int i = 0; i < names.length; i++) { names[i] = (String) namesJson.get(i); }

        OptionSpec.Builder builder = OptionSpec.builder(names);
        String type = (String) json.get("type");
        if (type != null) { builder.type(typeNameToClass(type)); }
        String[] description = readStringArray(json.get("description"));
        if (description != null) { builder.description(description); }
        Object defaultValue = json.get("defaultValue");
        if (defaultValue != null) { builder.defaultValue(String.valueOf(defaultValue)); }
        Object required = json.get("required");
        if (required != null) { builder.required((Boolean) required); }
        String arity = (String) json.get("arity");
        if (arity != null) { builder.arity(arity); }
        return builder.build();
    }

    private static PositionalParamSpec readPositional(Map<String, Object> json) {
        PositionalParamSpec.Builder builder = PositionalParamSpec.builder();
        String paramLabel = (String) json.get("paramLabel");
        if (paramLabel != null) { builder.paramLabel(paramLabel); }
        String type = (String) json.get("type");
        if (type != null) { builder.type(typeNameToClass(type)); }
        String[] description = readStringArray(json.get("description"));
        if (description != null) { builder.description(description); }
        Object defaultValue = json.get("defaultValue");
        if (defaultValue != null) { builder.defaultValue(String.valueOf(defaultValue)); }
        Object required = json.get("required");
        if (required != null) { builder.required((Boolean) required); }
        String arity = (String) json.get("arity");
        if (arity != null) { builder.arity(arity); }
        return builder.build();
    }

    // ---- writing: CommandSpec -> JSON ----

    private static Map<String, Object> writeCommand(CommandSpec spec) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("name", spec.name());
        putDescriptionIfPresent(json, spec.usageMessage().description());

        if (!spec.options().isEmpty()) {
            List<Object> options = new ArrayList<Object>();
            for (OptionSpec option : spec.options()) {
                options.add(writeOption(option));
            }
            json.put("options", options);
        }
        if (!spec.positionalParameters().isEmpty()) {
            List<Object> positionals = new ArrayList<Object>();
            for (PositionalParamSpec positional : spec.positionalParameters()) {
                positionals.add(writePositional(positional));
            }
            json.put("positionalParams", positionals);
        }
        if (!spec.subcommands().isEmpty()) {
            List<Object> subcommands = new ArrayList<Object>();
            for (CommandLine subcommand : spec.subcommands().values()) {
                subcommands.add(writeCommand(subcommand.getCommandSpec()));
            }
            json.put("subcommands", subcommands);
        }
        return json;
    }

    private static Map<String, Object> writeOption(OptionSpec option) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("names", new ArrayList<Object>(java.util.Arrays.asList(option.names())));
        putCommonArgSpecFields(json, option);
        return json;
    }

    private static Map<String, Object> writePositional(PositionalParamSpec positional) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("paramLabel", positional.paramLabel());
        putCommonArgSpecFields(json, positional);
        return json;
    }

    private static void putCommonArgSpecFields(Map<String, Object> json, picocli.CommandLine.Model.ArgSpec arg) {
        json.put("type", classToTypeName(arg.type()));
        putDescriptionIfPresent(json, arg.description());
        if (arg.defaultValue() != null) {
            json.put("defaultValue", arg.defaultValue());
        }
        if (arg.required()) {
            json.put("required", Boolean.TRUE);
        }
        json.put("arity", arg.arity().toString());
    }

    private static void putDescriptionIfPresent(Map<String, Object> json, String[] description) {
        if (description != null && description.length > 0) {
            json.put("description", new ArrayList<Object>(java.util.Arrays.asList((Object[]) description)));
        }
    }

    // ---- helpers ----

    private static String[] readStringArray(Object value) {
        if (value == null) { return null; }
        List<?> list = (List<?>) value;
        String[] result = new String[list.size()];
        for (int i = 0; i < result.length; i++) { result[i] = (String) list.get(i); }
        return result;
    }

    private static List<?> listOrEmpty(Object value) {
        return value == null ? java.util.Collections.emptyList() : (List<?>) value;
    }

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

    private static Class<?> typeNameToClass(String typeName) {
        Class<?> type = TYPES_BY_NAME.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type \"" + typeName + "\": supported types are " + TYPES_BY_NAME.keySet());
        }
        return type;
    }

    private static String classToTypeName(Class<?> type) {
        String name = NAMES_BY_TYPE.get(type);
        return name != null ? name : type.getSimpleName();
    }
}
