package picocli.jsonspec;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.jsonspec.json.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a picocli {@link CommandSpec} from JSON, and writes a {@link CommandSpec} to JSON.
 * <p>This lets a CLI be defined without an annotated Java class: for example, to give a
 * beautiful, help-and-completion-enabled picocli front end to a third-party tool whose own
 * CLI is not human-friendly, describe that tool's commands/options/positional parameters as
 * JSON (or write it directly in the {@link CommandSpecDsl lightweight DSL}) and build a
 * {@code CommandSpec} from it with {@link #read(String)}.</p>
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
        Map<String, Object> root = (Map<String, Object>) parsed;
        Definitions definitions = Definitions.from((Map<String, Object>) root.get("definitions"));
        return readCommand(root, definitions);
    }

    /**
     * The document-root {@code "definitions"} dictionary: named option/positional-param
     * templates that a command's own {@code options}/{@code positionalParams} array can
     * reference by name (a plain JSON string) instead of repeating the full definition. Only
     * {@link #read} understands this; {@link #write} always emits fully-inlined objects.
     */
    private static final class Definitions {
        static final Definitions EMPTY = new Definitions(java.util.Collections.<String, Map<String, Object>>emptyMap(),
                java.util.Collections.<String, Map<String, Object>>emptyMap());

        final Map<String, Map<String, Object>> options;
        final Map<String, Map<String, Object>> positionalParams;

        Definitions(Map<String, Map<String, Object>> options, Map<String, Map<String, Object>> positionalParams) {
            this.options = options;
            this.positionalParams = positionalParams;
        }

        @SuppressWarnings("unchecked")
        static Definitions from(Map<String, Object> json) {
            if (json == null) { return EMPTY; }
            return new Definitions(
                    (Map<String, Map<String, Object>>) (Map<String, ?>) mapOrEmpty(json.get("options")),
                    (Map<String, Map<String, Object>>) (Map<String, ?>) mapOrEmpty(json.get("positionalParams")));
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> mapOrEmpty(Object value) {
            return value == null ? java.util.Collections.<String, Object>emptyMap() : (Map<String, Object>) value;
        }

        Map<String, Object> resolveOption(String name) {
            Map<String, Object> def = options.get(name);
            if (def == null) {
                throw new IllegalArgumentException("Reference to undefined option \"" + name + "\": not found in \"definitions.options\"");
            }
            return def;
        }

        Map<String, Object> resolvePositional(String label) {
            Map<String, Object> def = positionalParams.get(label);
            if (def == null) {
                throw new IllegalArgumentException("Reference to undefined positional parameter \"" + label + "\": not found in \"definitions.positionalParams\"");
            }
            return def;
        }
    }

    /** Serializes the given {@link CommandSpec} (with any nested subcommands) to JSON text. */
    public static String write(CommandSpec spec) {
        return Json.write(writeCommand(spec));
    }

    // ---- reading: JSON -> CommandSpec ----

    @SuppressWarnings("unchecked")
    private static CommandSpec readCommand(Map<String, Object> json, Definitions definitions) {
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
            Map<String, Object> optionJson = option instanceof String
                    ? definitions.resolveOption((String) option)
                    : (Map<String, Object>) option;
            spec.addOption(readOption(optionJson));
        }
        for (Object positional : listOrEmpty(json.get("positionalParams"))) {
            Map<String, Object> positionalJson = positional instanceof String
                    ? definitions.resolvePositional((String) positional)
                    : (Map<String, Object>) positional;
            spec.addPositional(readPositional(positionalJson));
        }
        for (Object subcommand : listOrEmpty(json.get("subcommands"))) {
            Map<String, Object> subJson = (Map<String, Object>) subcommand;
            CommandSpec subSpec = readCommand(subJson, definitions);
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
        if (type != null) { builder.type(ArgTypes.toClass(type)); }
        String[] description = readStringArray(json.get("description"));
        if (description != null) { builder.description(description); }
        Object defaultValue = json.get("defaultValue");
        if (defaultValue != null) { builder.defaultValue(String.valueOf(defaultValue)); }
        Object required = json.get("required");
        if (required != null) { builder.required((Boolean) required); }
        String arity = (String) json.get("arity");
        if (arity != null) { builder.arity(arity); }
        Object usageHelp = json.get("usageHelp");
        if (usageHelp != null) { builder.usageHelp((Boolean) usageHelp); }
        Object versionHelp = json.get("versionHelp");
        if (versionHelp != null) { builder.versionHelp((Boolean) versionHelp); }
        return builder.build();
    }

    private static PositionalParamSpec readPositional(Map<String, Object> json) {
        PositionalParamSpec.Builder builder = PositionalParamSpec.builder();
        String paramLabel = (String) json.get("paramLabel");
        if (paramLabel != null) { builder.paramLabel(paramLabel); }
        String type = (String) json.get("type");
        if (type != null) { builder.type(ArgTypes.toClass(type)); }
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
        if (option.usageHelp()) { json.put("usageHelp", Boolean.TRUE); }
        if (option.versionHelp()) { json.put("versionHelp", Boolean.TRUE); }
        return json;
    }

    private static Map<String, Object> writePositional(PositionalParamSpec positional) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("paramLabel", positional.paramLabel());
        putCommonArgSpecFields(json, positional);
        return json;
    }

    private static void putCommonArgSpecFields(Map<String, Object> json, picocli.CommandLine.Model.ArgSpec arg) {
        json.put("type", ArgTypes.toName(arg.type()));
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
}
