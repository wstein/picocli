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
 * Sanity-checks {@code examples/flix-0.76.2.picocli} -- a realistic spec for the real flix CLI at
 * a much later version than {@link Flix0600ExampleTest}'s {@code flix-0.60.0.picocli}, curated from
 * reading flix's actual command implementations (Bootstrap.scala) at git tag v0.76.2 rather than
 * from a running {@code flixw --help}. See examples/README.md for the reasoning, including three
 * genuine, source-verified differences from how the 0.60.0 file curated the equivalent options.
 */
public class Flix0762ExampleTest {

    private static String readExample() throws IOException {
        File file = new File("examples/flix-0.76.2.picocli");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesIntoTheDocumentedCommandSet() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertEquals("flix", flix.name());
        assertEquals(new HashSet<String>(Arrays.asList(
                "init", "check", "build", "build-classes", "build-jar", "build-fatjar", "build-pkg",
                "clean", "doc", "format", "run", "test", "repl", "lsp", "lsp-vscode", "release",
                "install", "remove", "upgrade", "outdated", "stat", "eff-check", "eff-lock")),
                flix.subcommands().keySet());
    }

    /**
     * flix's own internal dev-only benchmark commands (Xperf/Xmemory/Xzhegalkin) are declared
     * {@code .hidden()} in scopt -- never shown even in flix's own {@code --help} -- so this proxy
     * deliberately never exposes them either, the same way it never fabricates commands the
     * wrapped tool doesn't present to users.
     */
    @Test
    public void hiddenDevOnlyCommandsAreNotExposed() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertFalse(flix.subcommands().containsKey("Xperf"));
        assertFalse(flix.subcommands().containsKey("Xmemory"));
        assertFalse(flix.subcommands().containsKey("Xzhegalkin"));
    }

    @Test
    public void topLevelOnlyHasTheOptionsThatWorkWithoutACommand() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        Set<String> topLevelOptionNames = new HashSet<String>();
        for (CommandLine.Model.OptionSpec option : flix.options()) { topLevelOptionNames.add(option.longestName()); }

        assertEquals(new HashSet<String>(Arrays.asList("--help", "--version", "--json", "--listen")), topLevelOptionNames);
    }

    /**
     * Regression check for a real, source-verified difference from flix-0.60.0.picocli: in
     * v0.76.2, Main.scala's Command.Build/Command.Run/Command.Repl branches explicitly reject any
     * file arguments ("The '...' command does not support file arguments."), unlike Command.Check/
     * Command.Doc/Command.Format/Command.Test which accept them as optional. The 0.60.0 file
     * attached a shared "files" positional to build/run/repl too; that would be wrong here.
     */
    @Test
    public void onlyCheckDocFormatAndTestAcceptFileArguments() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        for (String withFiles : Arrays.asList("check", "doc", "format", "test")) {
            assertTrue(withFiles + " should accept file arguments",
                    !flix.subcommands().get(withFiles).getCommandSpec().positionalParameters().isEmpty());
        }
        for (String withoutFiles : Arrays.asList("build", "run", "repl", "build-jar", "build-fatjar", "build-pkg")) {
            assertTrue(withoutFiles + " should not accept file arguments",
                    flix.subcommands().get(withoutFiles).getCommandSpec().positionalParameters().isEmpty());
        }
    }

    @Test
    public void runAndBuildJarAcceptEntrypointButBuildDoesNot() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        assertTrue(flix.subcommands().get("run").getCommandSpec().findOption("--entrypoint") != null);
        assertTrue(flix.subcommands().get("build-jar").getCommandSpec().findOption("--entrypoint") != null);
        assertTrue(flix.subcommands().get("build-fatjar").getCommandSpec().findOption("--entrypoint") != null);
        assertEquals(null, flix.subcommands().get("build").getCommandSpec().findOption("--entrypoint"));
    }

    @Test
    public void installRemoveUpgradeEachHaveARequiredPackagePositionalAndGithubToken() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        for (String cmd : Arrays.asList("install", "remove", "upgrade")) {
            CommandSpec spec = flix.subcommands().get(cmd).getCommandSpec();
            assertEquals(cmd, 1, spec.positionalParameters().size());
            assertTrue(cmd, spec.positionalParameters().get(0).required());
            assertTrue(cmd, spec.findOption("--github-token") != null);
        }
        // Only install prompts for confirmation (Bootstrap#install's selectMount); remove/upgrade don't.
        assertTrue(flix.subcommands().get("install").getCommandSpec().findOption("--yes") != null);
        assertEquals(null, flix.subcommands().get("remove").getCommandSpec().findOption("--yes"));
        assertEquals(null, flix.subcommands().get("upgrade").getCommandSpec().findOption("--yes"));
    }

    @Test
    public void lspVscodeHasARequiredPortPositional() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());

        CommandSpec lspVscode = flix.subcommands().get("lsp-vscode").getCommandSpec();
        assertEquals(1, lspVscode.positionalParameters().size());
        assertEquals("<port>", lspVscode.positionalParameters().get(0).paramLabel());
        assertTrue(lspVscode.positionalParameters().get(0).required());
    }

    @Test
    public void builtCommandLineActuallyParsesARealisticInvocation() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandLine cmd = new CommandLine(flix);

        ParseResult result = cmd.parseArgs("install", "flix/museum-clerk@1.1.0");

        assertTrue(result.hasSubcommand());
        ParseResult install = result.subcommand();
        assertEquals(1, install.matchedPositionals().size());
        assertEquals("flix/museum-clerk@1.1.0", install.matchedPositionalValue(0, (String) null));
    }

    /**
     * The 11 --X* flags are hidden (via a {@code hidden} group) and pulled into check/build/run/
     * test via "use xflags", matching flix-0.60.0.picocli's approach for its own 14-flag set.
     */
    @Test
    public void experimentalFlagsAreHiddenButStillFullyFunctional() throws IOException {
        CommandSpec flix = CommandSpecDsl.parse(readExample());
        CommandSpec check = flix.subcommands().get("check").getCommandSpec();

        assertTrue(check.argGroups().isEmpty());
        assertTrue(check.findOption("--Xnewmono").hidden());
        assertTrue(check.findOption("--Xhelp") != null);
        assertFalse(check.findOption("--Xhelp").hidden());

        CommandLine cmd = new CommandLine(flix);
        ParseResult result = cmd.parseArgs("check", "--Xverify", "--Xnewmono");
        ParseResult checkResult = result.subcommand();
        assertTrue(checkResult.matchedOptionValue("--Xverify", Boolean.FALSE));
        assertTrue(checkResult.matchedOptionValue("--Xnewmono", Boolean.FALSE));
    }
}
