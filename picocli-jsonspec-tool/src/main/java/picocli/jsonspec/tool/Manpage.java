package picocli.jsonspec.tool;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.codegen.docgen.manpage.ManPageGenerator;

import java.io.File;
import java.io.IOException;

/**
 * Generates AsciiDoc man pages for a {@link CommandSpec} and every one of its subcommands, via
 * picocli-codegen's {@link ManPageGenerator} -- which already accepts plain {@code CommandSpec}
 * instances, with no annotated Java class required, so it works directly against a spec built
 * from a picocli-jsonspec DSL/JSON file.
 */
final class Manpage {

    private Manpage() {
    }

    static int generate(CommandSpec spec, File outputDirectory, boolean[] verbosity) throws IOException {
        // ManPageGenerator calls spec.commandLine(), which a freshly-parsed root CommandSpec
        // doesn't have set yet -- addSubcommand() wraps every SUBCOMMAND spec in its own
        // CommandLine already, but nothing does that for the root until something wraps it.
        new CommandLine(spec);
        return ManPageGenerator.generateManPage(outputDirectory, null, verbosity, false, spec);
    }
}
