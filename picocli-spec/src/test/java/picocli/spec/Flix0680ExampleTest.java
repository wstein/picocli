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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Sanity-checks {@code examples/flix-0.68.0.picocli} -- verifies the command set and options
 * for flix v0.68.0 - v0.72.0, which added 'eff-check' and 'eff-lock', and removed '--explain'.
 */
public class Flix0680ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.68.0.picocli");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesIntoTheDocumentedCommandSet() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertEquals("flix", flix.name());
        assertEquals(new HashSet<String>(Arrays.asList(
                "init", "check", "build", "build-jar", "build-fatjar", "build-pkg", "clean", "doc", "format",
                "run", "test", "repl", "lsp", "lsp-vscode", "release", "outdated", "eff-check", "eff-lock")),
                flix.subcommands().keySet());
    }

    @Test
    public void effCommandsUseCompileOptions() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        CommandSpec effCheck = flix.subcommands().get("eff-check").getCommandSpec();
        assertTrue(effCheck.findOption("--threads") != null);
        assertTrue(effCheck.findOption("--github-token") != null);
        assertEquals(0, effCheck.positionalParameters().size());

        CommandSpec effLock = flix.subcommands().get("eff-lock").getCommandSpec();
        assertTrue(effLock.findOption("--threads") != null);
        assertTrue(effLock.findOption("--github-token") != null);
        assertEquals(0, effLock.positionalParameters().size());
    }

    @Test
    public void explainOptionIsRemoved() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec check = flix.subcommands().get("check").getCommandSpec();

        assertEquals(null, check.findOption("--explain"));
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
