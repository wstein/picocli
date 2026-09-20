package picocli.jsonspec;

import picocli.CommandLine.Model.CommandSpec;

/**
 * Merges one or more externally-defined {@link CommandSpec}s (typically read from JSON with
 * {@link CommandSpecJson#read(String)}) into a host ("uber") {@code CommandSpec}, each as a
 * subcommand keyed by its own {@link CommandSpec#name() name}.
 * <p>This is the composition step for a proxy CLI: a hand-built or annotation-based uber
 * command contributes its own top-level options/behavior, while one or more imported specs
 * each contribute a whole subcommand namespace describing a delegated tool's CLI (e.g. flix).
 * How a merged-in subcommand is actually executed (shelling out, an API call, etc.) is up to
 * the uber command's own execution logic -- this class only builds the combined model.</p>
 */
public final class CommandSpecMerger {

    private CommandSpecMerger() {}

    /**
     * Attaches each of the given {@code imported} specs to {@code uber} as a subcommand named
     * after {@link CommandSpec#name() imported.name()}, and returns {@code uber}.
     * @throws IllegalArgumentException if an imported spec has no name, if {@code uber}
     *      already has a subcommand with that name, or if merging introduces one of the two
     *      parsing ambiguities {@link SpecValidator} checks for (e.g. an imported spec's name
     *      colliding with an existing option's {@code defaultValue} on {@code uber})
     */
    public static CommandSpec merge(CommandSpec uber, CommandSpec... imported) {
        for (CommandSpec spec : imported) {
            String name = spec.name();
            if (name == null || CommandSpec.DEFAULT_COMMAND_NAME.equals(name)) {
                throw new IllegalArgumentException("Cannot merge an imported CommandSpec that has no name");
            }
            if (uber.subcommands().containsKey(name)) {
                throw new IllegalArgumentException("Command '" + uber.name() + "' already has a subcommand named '" + name + "'");
            }
            uber.addSubcommand(name, spec);
        }
        SpecValidator.validate(uber);
        return uber;
    }
}
