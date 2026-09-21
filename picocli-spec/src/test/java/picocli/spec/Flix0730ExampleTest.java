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
 * Sanity-checks {@code examples/flix-0.73.0.picocli} -- verifies the command set and options
 * for flix v0.73.0 - v0.75.1, which added '--top' to compileOptions.
 */
public class Flix0730ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.73.0.picocli");
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
    public void topOptionIsPresentInCompileOptions() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        CommandSpec build = flix.subcommands().get("build").getCommandSpec();
        assertTrue(build.findOption("--top") != null);
        assertTrue(build.findOption("--threads") != null);

        CommandSpec clean = flix.subcommands().get("clean").getCommandSpec();
        assertEquals(null, clean.findOption("--top"));
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
