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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Sanity-checks {@code examples/flix-0.75.3.picocli} -- verifies the command set and options
 * for flix v0.75.3, which added 'build-classes' and updated descriptions for 'clean' and '--Xprint-phases'.
 */
public class Flix0753ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.75.3.picocli");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesIntoTheDocumentedCommandSet() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertEquals("flix", flix.name());
        assertEquals(new HashSet<String>(Arrays.asList(
                "init", "check", "build", "build-classes", "build-jar", "build-fatjar", "build-pkg", "clean", "doc", "format",
                "run", "test", "repl", "lsp", "lsp-vscode", "release", "outdated", "eff-check", "eff-lock")),
                flix.subcommands().keySet());
    }

    @Test
    public void buildClassesUsesCompileOptions() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec buildClasses = flix.subcommands().get("build-classes").getCommandSpec();

        assertTrue(buildClasses.findOption("--threads") != null);
        assertTrue(buildClasses.findOption("--top") != null);
        assertEquals(0, buildClasses.positionalParameters().size());
    }

    @Test
    public void cleanHasUpdatedDescription() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec clean = flix.subcommands().get("clean").getCommandSpec();

        assertArrayEquals(new String[] {"removes the build directory (class files and generated documentation)."},
                clean.usageMessage().description());
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
