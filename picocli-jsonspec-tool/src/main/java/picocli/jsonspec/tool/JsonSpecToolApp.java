package picocli.jsonspec.tool;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * A picocli command line tool for working with picocli-jsonspec DSL/JSON files: preview the CLI a
 * spec produces, generate a shell completion script or AsciiDoc man pages for it, or just check
 * that it loads. Built with picocli itself in the ordinary, annotated way -- unlike the specs it
 * operates on, this outer tool is not itself described by a picocli-jsonspec file.
 */
@Command(name = "picocli-jsonspec", mixinStandardHelpOptions = true, version = "picocli-jsonspec-tool " + CommandLine.VERSION,
        subcommands = {PreviewCommand.class, CompletionCommand.class, ManpageCommand.class, ValidateCommand.class},
        description = "Preview, validate, and generate shell completion scripts and man pages "
                + "from picocli-jsonspec DSL/JSON spec files.")
public final class JsonSpecToolApp implements Runnable {

    @Spec
    CommandSpec spec;

    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new JsonSpecToolApp()).execute(args));
    }
}
