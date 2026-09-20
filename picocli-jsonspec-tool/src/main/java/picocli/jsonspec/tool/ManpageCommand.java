package picocli.jsonspec.tool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "manpage", mixinStandardHelpOptions = true,
        description = "Generates AsciiDoc man pages for the given spec and every one of its subcommands, "
                + "via picocli-codegen's ManPageGenerator.")
final class ManpageCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "<spec-file>", description = "A .picocli or .json spec file.")
    File specFile;

    @Option(names = {"-d", "--outdir"}, defaultValue = ".", paramLabel = "<dir>",
            description = "Output directory to write the generated AsciiDoc files to (default: the current directory).")
    File outputDirectory;

    @Option(names = {"-v", "--verbose"}, description = "Specify multiple -v options to increase verbosity.")
    boolean[] verbosity = new boolean[0];

    public Integer call() throws Exception {
        CommandSpec spec = SpecLoader.load(specFile);
        return Manpage.generate(spec, outputDirectory, verbosity);
    }
}
