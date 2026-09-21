package picocli.spec;

import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Sanity-checks {@code examples/flix-0.67.1.picocli} -- verifies the command set and options
 * for flix v0.67.1 - v0.67.2, which added 'format', restricted file arguments to check/doc/format/test,
 * removed --args, and removed 4 experimental flags.
 */
public class Flix0671ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.67.1.picocli");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesIntoTheDocumentedCommandSet() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertEquals("flix", flix.name());
        assertEquals(new HashSet<String>(Arrays.asList(
                "init", "check", "build", "build-jar", "build-fatjar", "build-pkg", "clean", "doc", "format",
                "run", "test", "repl", "lsp", "lsp-vscode", "release", "outdated")),
                flix.subcommands().keySet());
    }

    @Test
    public void formatCommandIsPresentAndAcceptsFiles() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec format = flix.subcommands().get("format").getCommandSpec();

        assertTrue(format.findOption("--threads") != null);
        assertEquals(1, format.positionalParameters().size());
        assertEquals("<files>", format.positionalParameters().get(0).paramLabel());
    }

    @Test
    public void onlyCheckDocFormatAndTestAcceptFiles() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        for (String subName : Arrays.asList("check", "doc", "format", "test")) {
            CommandSpec sub = flix.subcommands().get(subName).getCommandSpec();
            assertEquals(subName + " should accept files", 1, sub.positionalParameters().size());
        }

        for (String subName : Arrays.asList("build", "run", "repl", "clean", "init", "release", "outdated")) {
            CommandSpec sub = flix.subcommands().get(subName).getCommandSpec();
            assertEquals(subName + " should not accept files", 0, sub.positionalParameters().size());
        }
    }

    @Test
    public void argsOptionIsRemovedFromRun() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec run = flix.subcommands().get("run").getCommandSpec();

        assertEquals(null, run.findOption("--args"));
        assertTrue(run.findOption("--entrypoint") != null);
    }

    @Test
    public void experimentalFlagsCountIsTen() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec check = flix.subcommands().get("check").getCommandSpec();

        assertEquals(1, check.argGroups().size());
        assertEquals(10, check.argGroups().get(0).args().size());
        assertEquals(null, check.findOption("--Xfuzzer"));
        assertEquals(null, check.findOption("--Xchaos-monkey"));
    }

    @Test
    public void roundTripsFlixSpec() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        String dsl = CommandSpecDsl.write(flix);
        CommandSpec roundTripped = CommandSpecDsl.parse(dsl);

        assertEquals(flix.name(), roundTripped.name());
        assertEquals(flix.subcommands().keySet(), roundTripped.subcommands().keySet());
    }
}
