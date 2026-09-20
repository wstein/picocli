package picocli.jsonspec.tool;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Prints the usage help message for a {@link CommandSpec} and, recursively, every one of its
 * subcommands -- so an author can see exactly what CLI a picocli-jsonspec file produces without
 * writing any Java or wiring up execution.
 */
final class Preview {

    private Preview() {
    }

    static void render(CommandSpec spec, PrintWriter out, Ansi ansi) {
        render(new CommandLine(spec), out, ansi, new LinkedHashSet<CommandLine>());
    }

    private static void render(CommandLine cmd, PrintWriter out, Ansi ansi, Set<CommandLine> done) {
        if (!done.add(cmd)) {
            return; // an alias resolves to the same CommandLine instance as its primary name; don't print it twice
        }
        cmd.usage(out, ansi);
        for (CommandLine sub : cmd.getSubcommands().values()) {
            out.println();
            render(sub, out, ansi, done);
        }
    }
}
