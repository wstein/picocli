package picocli.spec;

import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

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
 * Sanity-checks {@code examples/flix-0.67.0.picocli} -- verifies the command set and options
 * for flix v0.67.0, which added the 'clean' subcommand.
 */
public class Flix0670ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.67.0.picocli");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesIntoTheDocumentedCommandSet() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertEquals("flix", flix.name());
        assertEquals(new HashSet<String>(Arrays.asList(
                "init", "check", "build", "build-jar", "build-fatjar", "build-pkg", "clean", "doc",
                "run", "test", "repl", "lsp", "lsp-vscode", "release", "outdated")),
                flix.subcommands().keySet());
    }

    @Test
    public void cleanCommandIsPresentAndUsesDependencyResolution() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec clean = flix.subcommands().get("clean").getCommandSpec();

        assertTrue(clean.findOption("--github-token") != null);
        assertTrue(clean.findOption("--no-install") != null);
        assertEquals(null, clean.findOption("--threads"));
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
