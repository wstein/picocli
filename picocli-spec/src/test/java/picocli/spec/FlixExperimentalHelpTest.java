package picocli.spec;

import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Demonstrates the runtime half of {@code examples/flix-0.60.0.picocli}'s {@code --Xhelp}:
 * printing a subcommand's hidden {@code --X*} options on demand. Which options count as
 * "experimental" (hidden + a {@code --X} name) and how to render them can't be expressed in the
 * declarative spec itself -- a host CLI's own execution strategy decides what to do when
 * {@code --Xhelp} is matched, the same way it decides what to do for any other matched option.
 * This test exercises exactly the logic a real proxy would wire up.
 */
public class FlixExperimentalHelpTest {

    /** What a host CLI's dispatch logic would call upon seeing "--Xhelp" matched for a subcommand. */
    private static void printExperimentalOptions(CommandLine subcommand) {
        CommandSpec spec = subcommand.getCommandSpec();
        List<OptionSpec> experimental = new ArrayList<OptionSpec>();
        for (OptionSpec option : spec.options()) {
            if (option.hidden() && option.longestName().startsWith("--X")) {
                experimental.add(option);
            }
        }
        Help help = new Help(spec, Help.defaultColorScheme(Help.Ansi.OFF));
        subcommand.getOut().print(help.optionListExcludingGroups(experimental));
        subcommand.getOut().flush(); // PrintWriter autoFlush only triggers on println/printf/format, not plain print()
    }

    private static PrintWriter utf8Writer(ByteArrayOutputStream out) throws UnsupportedEncodingException {
        return new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true);
    }

    @Test
    public void xhelpPrintsOnlyTheHiddenExperimentalOptionsForThatCommand() throws IOException {
        File file = new File("examples/flix-0.60.0.picocli");
        String dsl = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        CommandSpec flix = CommandSpecDsl.parse(dsl);
        CommandLine cmd = new CommandLine(flix);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cmd.setOut(utf8Writer(out));

        ParseResult result = cmd.parseArgs("check", "--Xhelp");
        ParseResult checkResult = result.subcommand();
        assertTrue(checkResult.matchedOptionValue("--Xhelp", Boolean.FALSE));

        printExperimentalOptions(checkResult.commandSpec().commandLine());

        String printed = out.toString("UTF-8");
        assertTrue(printed.contains("--Xfuzzer"));
        assertTrue(printed.contains("enables compiler fuzzing"));
        assertTrue(printed.contains("--Xiterations"));
        assertFalse("a non-experimental option must not be printed", printed.contains("--explain"));
        assertFalse("--Xhelp itself is not hidden, so it must not be printed here", printed.contains("--Xhelp"));
    }

    @Test
    public void commandsWithoutXflagsHaveNoExperimentalOptionsToPrint() throws IOException {
        File file = new File("examples/flix-0.60.0.picocli");
        String dsl = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        CommandSpec flix = CommandSpecDsl.parse(dsl);
        CommandLine cmd = new CommandLine(flix);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cmd.setOut(utf8Writer(out));

        ParseResult result = cmd.parseArgs("doc");
        printExperimentalOptions(result.subcommand().commandSpec().commandLine());

        assertTrue(out.toString("UTF-8").isEmpty());
    }
}
