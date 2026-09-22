package picocli.spec.tool;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Prints the usage help message for a {@link CommandSpec} and, recursively, every one of its
 * subcommands -- so an author can see exactly what CLI a picocli-spec file produces without
 * writing any Java or wiring up execution. Optionally also renders one or more of the command's
 * tagged on-demand {@linkplain CommandSpec#helpSections() help sections} (e.g. "experimental"),
 * for every command in the tree that declares that section.
 */
final class Preview {

    private Preview() {
    }

    static void render(CommandSpec spec, PrintWriter out, Ansi ansi) {
        render(spec, out, ansi, Collections.<String>emptyList(), null);
    }

    /**
     * @param sections      names of on-demand help sections to additionally render for every command
     *                      that declares them (e.g. {@code "experimental"}); empty to render only
     *                      the standard usage help
     * @param triggerOption an explicit option to pass when rendering a section, overriding automatic
     *                      discovery of the {@code usageHelp} option tagged with that section; {@code null}
     *                      to auto-discover the trigger for each command
     */
    static void render(CommandSpec spec, PrintWriter out, Ansi ansi, List<String> sections, String triggerOption) {
        CommandLine root = new CommandLine(spec);
        render(root, root, Collections.<String>emptyList(), out, ansi, sections, triggerOption, new LinkedHashSet<CommandLine>());
    }

    private static void render(CommandLine root, CommandLine cmd, List<String> path, PrintWriter out, Ansi ansi,
                                List<String> sections, String triggerOption, Set<CommandLine> done) {
        if (!done.add(cmd)) {
            return; // an alias resolves to the same CommandLine instance as its primary name; don't print it twice
        }
        cmd.setColorScheme(CommandLine.Help.defaultColorScheme(ansi)); // keep execute()'s help rendering in sync with the --ansi setting used for usage(out, ansi) below
        cmd.usage(out, ansi);
        renderRequestedSections(root, cmd, path, out, sections, triggerOption);
        for (Map.Entry<String, CommandLine> entry : cmd.getSubcommands().entrySet()) {
            out.println();
            List<String> childPath = new ArrayList<String>(path);
            childPath.add(entry.getKey());
            render(root, entry.getValue(), childPath, out, ansi, sections, triggerOption, done);
        }
    }

    private static void renderRequestedSections(CommandLine root, CommandLine cmd, List<String> path, PrintWriter out,
                                                  List<String> sections, String triggerOption) {
        CommandSpec spec = cmd.getCommandSpec();
        for (String section : sections) {
            if (!spec.helpSections().contains(section)) {
                continue; // this command has no content tagged with this section
            }
            String trigger = triggerOption != null ? triggerOption : findTrigger(spec, section);
            out.println();
            if (trigger != null) {
                renderViaExecute(root, cmd, path, out, trigger);
            } else {
                cmd.printHelpSection(section, out); // no usageHelp trigger declared for this section; render its raw content directly
            }
        }
    }

    private static String findTrigger(CommandSpec spec, String section) {
        Optional<OptionSpec> trigger = spec.findHelpSectionTrigger(section);
        return trigger.isPresent() ? trigger.get().longestName() : null;
    }

    /**
     * Executes the trigger option from the root command down the given subcommand path, so that
     * picocli's parent-chain bookkeeping (which requires every ancestor to have actually been
     * parsed) stays consistent -- calling {@code execute()} directly on a subcommand's own,
     * never-parsed-from-the-root {@code CommandLine} throws deep inside the parser.
     */
    private static void renderViaExecute(CommandLine root, CommandLine leaf, List<String> path, PrintWriter out, String trigger) {
        PrintWriter previousOut = leaf.getOut(); // picocli prints on-demand help to the matched (leaf) command's own configured writer, not the root's
        leaf.setOut(out);
        try {
            List<String> args = new ArrayList<String>(path);
            args.add(trigger);
            root.execute(args.toArray(new String[0]));
        } finally {
            leaf.setOut(previousOut);
        }
    }
}
