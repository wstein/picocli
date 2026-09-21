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
 * Sanity-checks {@code examples/flix-0.75.2.picocli} -- verifies the command set and options
 * for flix v0.75.2, which added '--Xnewmono' to experimental flags.
 */
public class Flix0752ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.75.2.picocli");
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
    public void xnewmonoOptionIsPresentInExperimentalFlags() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec check = flix.subcommands().get("check").getCommandSpec();

        assertEquals(1, check.argGroups().size());
        assertEquals(11, check.argGroups().get(0).args().size());
        assertTrue(check.findOption("--Xnewmono") != null);
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
