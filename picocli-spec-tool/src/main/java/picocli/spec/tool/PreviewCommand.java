package picocli.spec.tool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "preview", mixinStandardHelpOptions = true,
        description = "Prints the usage help message for the given spec, and every subcommand, recursively.")
final class PreviewCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "<spec-file>", description = "A .picocli or .json spec file.")
    File specFile;

    @Option(names = "--ansi", description = "Force ANSI colors on/off/auto (default: auto).", defaultValue = "AUTO")
    Ansi ansi;

    @Spec
    CommandSpec self;

    public Integer call() throws Exception {
        CommandSpec spec = SpecLoader.load(specFile);
        Preview.render(spec, self.commandLine().getOut(), ansi);
        self.commandLine().getOut().flush(); // PrintWriter autoFlush only triggers on println/printf/format
        return 0;
    }
}
