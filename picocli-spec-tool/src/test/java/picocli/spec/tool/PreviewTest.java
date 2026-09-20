package picocli.spec.tool;

import org.junit.Test;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.spec.CommandSpecDsl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreviewTest {

    private static String render(CommandSpec spec) throws UnsupportedEncodingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true);
        Preview.render(spec, writer, Ansi.OFF);
        writer.flush();
        return out.toString("UTF-8");
    }

    @Test
    public void printsTheRootCommandsUsageHelp() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo \"A demo tool.\" {\n" +
                "  option --verbose : boolean \"be verbose.\"\n" +
                "}");

        String printed = render(spec);

        assertTrue(printed.contains("A demo tool."));
        assertTrue(printed.contains("--verbose"));
    }

    @Test
    public void recursivelyPrintsEverySubcommandsUsageHelp() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo \"A demo tool.\" {\n" +
                "  command build \"Builds the thing.\" {\n" +
                "    option --release : boolean \"optimize for release.\"\n" +
                "  }\n" +
                "  command run \"Runs the thing.\" {}\n" +
                "}");

        String printed = render(spec);

        assertTrue(printed.contains("Builds the thing."));
        assertTrue(printed.contains("--release"));
        assertTrue(printed.contains("Runs the thing."));
    }

    @Test
    public void separatesEachCommandsUsageWithABlankLine() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo {\n" +
                "  command build {}\n" +
                "}");

        String printed = render(spec);

        assertTrue(printed.contains("\n\n"));
    }

    /**
     * "leaf command." legitimately appears twice: once as "b"'s one-line summary in "a"'s own
     * subcommand list, and once as "b"'s own full description -- that's correct picocli behavior,
     * not a duplicate. What must NOT be duplicated is "b"'s own dedicated usage block, which only
     * {@link Preview}'s alias-deduplication (via identity, not text) could otherwise double-print.
     */
    @Test
    public void deeplyNestedSubcommandsGetExactlyOneOwnUsageBlockEach() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo {\n" +
                "  command a {\n" +
                "    command b \"leaf command.\" {}\n" +
                "  }\n" +
                "}");

        String printed = render(spec);

        int occurrences = printed.split("Usage: demo a b", -1).length - 1;
        assertEquals(1, occurrences);
    }
}
